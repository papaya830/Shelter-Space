#!/usr/bin/env bash
set -euo pipefail

base_url="${1:-http://localhost:8080}"
turnaway_count="${TURNAWAY_COUNT:-24}"
demand_per_type="${DEMAND_PER_TYPE:-6}"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required." >&2
  exit 1
fi
if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required." >&2
  exit 1
fi
if ! [[ "$turnaway_count" =~ ^[0-9]+$ ]] || ! [[ "$demand_per_type" =~ ^[0-9]+$ ]]; then
  echo "TURNAWAY_COUNT and DEMAND_PER_TYPE must be non-negative integers." >&2
  exit 1
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

http_status="$(curl -sS -o "$tmp_dir/shelters.json" -w '%{http_code}' "$base_url/api/shelters")"
if [[ "$http_status" != "200" ]]; then
  echo "Could not load shelters from $base_url (HTTP $http_status). Is the app running?" >&2
  exit 1
fi

http_status="$(curl -sS -o "$tmp_dir/bookings.json" -w '%{http_code}' "$base_url/api/bookings")"
if [[ "$http_status" != "200" ]]; then
  echo "Could not load guest profiles from the booking queue (HTTP $http_status)." >&2
  exit 1
fi

shelter_ids=()
while IFS= read -r shelter_id; do
  shelter_ids+=("$shelter_id")
done < <(jq -r '.[] | select(.operationalStatus != "TEMPORARILY_CLOSED") | .id' "$tmp_dir/shelters.json")

if [[ "${#shelter_ids[@]}" -eq 0 ]]; then
  echo "No operational shelters were found." >&2
  exit 1
fi

guest_ids=()
while IFS= read -r guest_id; do
  guest_ids+=("$guest_id")
done < <(jq -r '[.[].guest.id] | unique[]' "$tmp_dir/bookings.json")

if [[ "${#guest_ids[@]}" -eq 0 ]]; then
  echo "No guest profiles were found in the booking queue. Start the app with demo seeding enabled first." >&2
  exit 1
fi

reasons=(
  "NO_BEDS_AVAILABLE"
  "INTAKE_CLOSED"
  "INELIGIBLE"
  "BEHAVIOUR_RESTRICTION"
  "REFERRED_ELSEWHERE"
  "OTHER"
)

reason_notes=(
  "Capacity was reached before intake could be completed."
  "Guest arrived after the published intake window."
  "Guest did not meet this shelter's current intake criteria."
  "Staff could not safely complete intake at this time."
  "Guest was connected with a more suitable shelter or service."
  "Guest left before intake was completed."
)

created_turnaways=0
i=0
while [[ "$i" -lt "$turnaway_count" ]]; do
  shelter_index=$((i % ${#shelter_ids[@]}))
  guest_index=$((i % ${#guest_ids[@]}))
  reason_index=$((i % ${#reasons[@]}))
  payload="$(jq -nc \
    --argjson shelterId "${shelter_ids[$shelter_index]}" \
    --argjson guestId "${guest_ids[$guest_index]}" \
    --arg reason "${reasons[$reason_index]}" \
    --arg notes "${reason_notes[$reason_index]}" \
    '{shelterId:$shelterId,guestId:$guestId,reason:$reason,recordedBy:"Demo Data Seeder",notes:$notes}')"
  http_status="$(curl -sS -o "$tmp_dir/turnaway-$i.json" -w '%{http_code}' \
    -H 'Content-Type: application/json' -d "$payload" "$base_url/api/turn-away-logs")"
  if [[ "$http_status" != "201" ]]; then
    echo "Turn-away record $((i + 1)) failed (HTTP $http_status):" >&2
    cat "$tmp_dir/turnaway-$i.json" >&2
    exit 1
  fi
  created_turnaways=$((created_turnaways + 1))
  i=$((i + 1))
done

population_types=(
  "ANY_GENDER"
  "MEN_ONLY"
  "WOMEN_ONLY"
  "FAMILY_ONLY"
  "WOMEN_WITH_CHILDREN"
  "YOUTH_ONLY"
)

created_demand=0
for population_type in "${population_types[@]}"; do
  i=1
  while [[ "$i" -le "$demand_per_type" ]]; do
    device_id="demo-demand-${population_type}-${i}"
    payload="$(jq -nc --arg deviceId "$device_id" --arg populationType "$population_type" \
      '{deviceId:$deviceId,populationType:$populationType}')"
    http_status="$(curl -sS -o "$tmp_dir/demand-${population_type}-${i}.json" -w '%{http_code}' \
      -H 'Content-Type: application/json' -d "$payload" "$base_url/api/analytics/interest")"
    if [[ "$http_status" != "204" ]]; then
      echo "Demand signal $population_type/$i failed (HTTP $http_status)." >&2
      exit 1
    fi
    created_demand=$((created_demand + 1))
    i=$((i + 1))
  done
done

curl -sS "$base_url/api/turn-away-logs" > "$tmp_dir/all-turnaways.json"
curl -sS "$base_url/api/analytics/interest/summary" > "$tmp_dir/demand-summary.json"

echo "Created $created_turnaways turn-away records linked across ${#guest_ids[@]} guest profiles."
echo "Submitted $created_demand demand signals (repeat runs keep device/type pairs unique)."
echo "Database now has $(jq 'length' "$tmp_dir/all-turnaways.json") turn-away records."
echo "Demand summary:"
jq -r '.[] | "  \(.populationType): \(.uniqueDevices) unique devices"' "$tmp_dir/demand-summary.json"
