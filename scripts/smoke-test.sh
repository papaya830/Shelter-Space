#!/usr/bin/env bash
set -euo pipefail

base_url="${1:-http://localhost:8080}"
today="$(date +%F)"
suffix="$(date +%s)"

require_status() {
  local expected="$1"
  local actual="$2"
  local label="$3"
  if [[ "$actual" != "$expected" ]]; then
    echo "FAIL: $label (expected HTTP $expected, got $actual)" >&2
    exit 1
  fi
  echo "PASS: $label"
}

request_json() {
  local method="$1"
  local path="$2"
  local payload="${3:-}"
  local output_file="$4"
  if [[ -n "$payload" ]]; then
    curl -sS -o "$output_file" -w '%{http_code}' -X "$method" \
      -H 'Content-Type: application/json' -d "$payload" "$base_url$path"
  else
    curl -sS -o "$output_file" -w '%{http_code}' -X "$method" "$base_url$path"
  fi
}

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

status="$(request_json GET /api/shelters '' "$tmp_dir/shelters.json")"
require_status 200 "$status" "load shelters"

available_id="$(jq -r '[.[] | select(.availableBeds > 0 and .operationalStatus != "TEMPORARILY_CLOSED")][0].id' "$tmp_dir/shelters.json")"
full_waitlist_id="$(jq -r '[.[] | select(.availableBeds == 0 and .supportsWaitlist == true and .operationalStatus != "TEMPORARILY_CLOSED")][0].id' "$tmp_dir/shelters.json")"

public_payload="$(jq -nc --argjson shelterId "$available_id" --arg name "Smoke Guest $suffix" --arg date "$today" \
  '{shelterId:$shelterId,displayName:$name,requestedBedDate:$date,requestedBy:"Automated smoke test",intakeNotes:"Lifecycle smoke test"}')"
status="$(request_json POST /api/bookings/public "$public_payload" "$tmp_dir/public.json")"
require_status 201 "$status" "public shelter registration"
booking_id="$(jq -r '.id' "$tmp_dir/public.json")"

decision='{"staffName":"Smoke Test Staff","notes":"Automated lifecycle verification"}'
status="$(request_json POST "/api/bookings/$booking_id/admit" "$decision" "$tmp_dir/admit.json")"
require_status 200 "$status" "staff admits booking"
status="$(request_json POST "/api/bookings/$booking_id/check-in" "$decision" "$tmp_dir/checkin.json")"
require_status 200 "$status" "staff checks guest in"
status="$(request_json POST "/api/bookings/$booking_id/check-out" "$decision" "$tmp_dir/checkout.json")"
require_status 200 "$status" "staff checks guest out"

for action in waitlist reject cancel; do
  case "$action" in
    waitlist) action_label="Waitlist" ;;
    reject) action_label="Reject" ;;
    cancel) action_label="Cancel" ;;
  esac
  action_payload="$(jq -nc --argjson shelterId "$available_id" --arg name "$action_label Guest $suffix" --arg date "$today" \
    '{shelterId:$shelterId,displayName:$name,requestedBedDate:$date,requestedBy:"Automated smoke test"}')"
  status="$(request_json POST /api/bookings/public "$action_payload" "$tmp_dir/$action-create.json")"
  require_status 201 "$status" "create booking for staff $action"
  action_booking_id="$(jq -r '.id' "$tmp_dir/$action-create.json")"
  status="$(request_json POST "/api/bookings/$action_booking_id/$action" "$decision" "$tmp_dir/$action.json")"
  require_status 200 "$status" "staff ${action}s booking"
done

waitlist_payload="$(jq -nc --argjson shelterId "$full_waitlist_id" --arg name "Waitlist Guest $suffix" --arg date "$today" \
  '{shelterId:$shelterId,displayName:$name,requestedBedDate:$date,requestedBy:"Automated smoke test"}')"
status="$(request_json POST /api/bookings/public/waitlist "$waitlist_payload" "$tmp_dir/waitlist.json")"
require_status 201 "$status" "public joins a full shelter waitlist"
[[ "$(jq -r '.status' "$tmp_dir/waitlist.json")" == "WAITLISTED" ]] || { echo 'FAIL: waitlist status' >&2; exit 1; }

turnaway_payload="$(jq -nc --argjson shelterId "$available_id" \
  '{shelterId:$shelterId,reason:"REFERRED_ELSEWHERE",recordedBy:"Smoke Test Staff",notes:"Automated smoke test record"}')"
status="$(request_json POST /api/turn-away-logs "$turnaway_payload" "$tmp_dir/turnaway.json")"
require_status 201 "$status" "staff records a turn-away"

status="$(request_json GET /api/bookings '' "$tmp_dir/bookings.json")"
require_status 200 "$status" "staff booking queue reload"
status="$(request_json GET /api/turn-away-logs '' "$tmp_dir/turnaways.json")"
require_status 200 "$status" "turn-away history reload"

analytics_payload="$(jq -nc --arg deviceId "smoke-$suffix" '{deviceId:$deviceId,populationType:"YOUTH_ONLY"}')"
status="$(request_json POST /api/analytics/interest "$analytics_payload" "$tmp_dir/analytics.json")"
require_status 204 "$status" "public demand filter analytics"
status="$(request_json GET /api/analytics/interest/summary '' "$tmp_dir/analytics-summary.json")"
require_status 200 "$status" "staff demand summary reload"

chat_payload="$(jq -nc --arg sessionId "smoke-$suffix" '{clientSessionId:$sessionId,message:"HELP"}')"
status="$(request_json POST /api/chatbot/messages "$chat_payload" "$tmp_dir/chat.json")"
require_status 200 "$status" "chatbot help flow"

echo "Smoke test complete: public registration, public/staff waitlists, admit, check-in, check-out, reject, cancel, queue, turn-away, analytics, and chatbot flows passed."
