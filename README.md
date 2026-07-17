# Shelter-Space

Shelter-Space is a Spring Boot MVP for shared shelter operations. It now includes bundled public and staff frontends served from the same Spring Boot app for shelter discovery, booking requests, intake review, and shelter operations.

## Tech Stack

- Java 17
- Spring Boot 3
- Gradle
- Spring Data JPA
- H2 in-memory database for local development
- bundled static single-page public and staff frontends served by Spring Boot

## Current MVP Scope

The current build includes:

- shelter records and public shelter listing data
- shelter capacity and derived bed availability
- minimal guest profile data
- booking request and admission lifecycle logic with REST endpoints
- optional turn-away logging
- local seed data for development and testing
- bundled public shelter UI at `/`
- bundled staff console at `/#/staff/dashboard`

## Frontend Access

The app serves both interfaces from the same Spring Boot host:

- public UI: `http://localhost:8080/`
- staff UI: `http://localhost:8080/#/staff/dashboard`
- API: `http://localhost:8080/api`

If port `8080` is already in use, run on a different port:

```bash
./gradlew bootRun --args='--server.port=8081'
```

Then use the same paths on that port.

## Public Frontend

The public-facing UI is designed for shelter users or helpers on phones, older devices, or low-friction browsing sessions.

Included MVP screens:

1. `Shelter List`
   - browses all shelters from `GET /api/shelters`
   - shows availability, operational status, population served, barrier level, and key accessibility flags
   - supports simple search plus filters for available beds, wheelchair access, and pets
2. `Shelter Detail`
   - shows intake instructions, intake type, intake window, rules, programs, perks, age limits, max stay, and service population
   - makes availability and intake-related flags easy to scan
3. `Public Booking Request`
   - submits a minimal request to the live backend using a public booking endpoint
   - collects alias/display name plus optional legal name, phone number, birth date, helper name, and intake notes
   - uses backend validation responses for clear error handling

Public booking flow:

1. open the public shelter list
2. choose a shelter and review the detail screen
3. open `Request a bed`
4. submit the minimal guest profile and requested bed date
5. the request is created in the existing booking queue for staff review

## Staff Frontend

The staff console is served from the same Spring Boot app as the API.

Included MVP screens:

1. `Booking Queue`
   - live booking table for staff review
   - status chips for `REQUESTED`, `WAITLISTED`, `ADMITTED`, `CHECKED_IN`, `REJECTED`, `CANCELLED`, and `CHECKED_OUT`
   - admit, reject, check-in, and check-out actions wired to the existing booking API
   - API success and error feedback surfaced in the UI
2. `Shelter Availability`
   - scanable shelter cards with capacity, occupancy, and available beds
   - shelter status, intake type, barrier level, and population served
   - visual emphasis for nearly full and full shelters
3. `Shelter Config`
   - edit existing shelter details against `PUT /api/shelters/{id}`
   - validation errors mapped from backend field responses
   - occupancy is shown as read-only because the current shelter update API does not accept direct occupancy edits

The staff UI remains available without a separate frontend toolchain and now coexists with the public-facing shelter flow.

## Running Locally

### Start the app

```bash
./gradlew bootRun
```

The app starts on:

- `http://localhost:8080`

### Run tests

```bash
./gradlew test
```

## How The Frontend Talks To The Backend

- the frontend is shipped from `src/main/resources/static`
- it uses browser `fetch` calls to the existing same-origin REST endpoints
- no mock data is used in the shipped public or staff workflows
- no frontend environment variables are required for local development because the UI and API are served from the same Spring Boot host

Main API integrations:

- `GET /api/bookings`
- `POST /api/bookings/public`
- `POST /api/bookings/{id}/admit`
- `POST /api/bookings/{id}/reject`
- `POST /api/bookings/{id}/check-in`
- `POST /api/bookings/{id}/check-out`
- `GET /api/shelters`
- `PUT /api/shelters/{id}`

## Local Database

The app uses an H2 in-memory database in local development.

H2 console:

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:shelterdb`
- Username: `sa`
- Password: blank

Important local behavior:

- schema is recreated on startup via `spring.jpa.hibernate.ddl-auto=create-drop`
- seed data loads automatically when `app.seed.enabled=true`

The frontend is intended to work against this seeded local dataset for demos and manual review.

Public booking requests also create new guest profiles in that same local dataset for staff review.

## Shelter API

Base path:

- `/api/shelters`

### `GET /api/shelters`

Returns all shelters for the current public listing view.

Response includes:

- id, name, organization name
- city and address
- operational status
- barrier level
- population type
- intake type and intake times
- intake instructions
- total capacity
- current occupancy
- derived `availableBeds`
- boolean support flags
- rules, programs, perks, notes

### `GET /api/shelters/{id}`

Returns a single shelter by id.

Behavior:

- `200 OK` when found
- `404 Not Found` when missing

### `POST /api/shelters`

Creates a new shelter.

Behavior:

- validates request body
- returns `201 Created`
- returns `Location` header for the created resource

### `PUT /api/shelters/{id}`

Updates an existing shelter.

Behavior:

- validates request body
- returns `200 OK` when updated
- returns `404 Not Found` when the shelter does not exist
- returns `409 Conflict` if capacity is set below current occupancy

### `DELETE /api/shelters/{id}`

Deletes a shelter.

Behavior:

- returns `204 No Content` when deleted
- returns `404 Not Found` when the shelter does not exist

## Booking API

Base path:

- `/api/bookings`

### `POST /api/bookings`

Creates a booking request.

Required request fields:

- `shelterId`
- `guestId`
- `requestedBedDate`
- `requestChannel`

Behavior:

- validates input
- returns `201 Created`
- returns `Location` header for the created booking
- blocks duplicate active booking lifecycles for the same guest

### `GET /api/bookings`

Returns bookings for staff review.

Response includes:

- booking id and status
- request channel
- requested bed date
- requested / decided / checked-in / checked-out timestamps
- decision and intake notes
- minimal guest summary
- shelter summary including derived `availableBeds`

### `GET /api/bookings/{id}`

Returns one booking by id.

Behavior:

- `200 OK` when found
- `404 Not Found` when missing

### `POST /api/bookings/{id}/admit`

Admits a requested or waitlisted booking.

Behavior:

- returns `200 OK`
- increments shelter occupancy
- returns `409 Conflict` for full shelters or invalid transitions

### `POST /api/bookings/{id}/reject`

Rejects a requested or waitlisted booking.

### `POST /api/bookings/{id}/check-in`

Checks in an admitted booking.

### `POST /api/bookings/{id}/check-out`

Checks out an admitted or checked-in booking.

Behavior:

- returns `200 OK`
- decrements shelter occupancy

### `POST /api/bookings/{id}/cancel`

Cancels a requested or waitlisted booking.

## Booking Workflow

Primary lifecycle states:

- `REQUESTED`
- `WAITLISTED`
- `ADMITTED`
- `REJECTED`
- `CHECKED_IN`
- `CHECKED_OUT`
- `CANCELLED`

Supported workflow:

1. Create booking with `POST /api/bookings`
2. Review with `GET /api/bookings`
3. Admit or reject with `/admit` or `/reject`
4. Check in with `/check-in`
5. Check out with `/check-out`

Example create request:

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "shelterId": 1,
    "guestId": 3,
    "requestedBedDate": "2026-07-18",
    "requestChannel": "PHONE",
    "requestedBy": "Front Desk",
    "intakeNotes": "Needs lower bunk if available"
  }'
```

## Error Response Shape

Validation, not-found, and conflict responses use a consistent JSON shape with:

- `timestamp`
- `status`
- `error`
- `message`
- `path`

Validation failures also include:

- `fields`

The staff frontend uses those `fields` values to show inline validation errors on the shelter edit screen.

The public booking form uses the same field-level validation response shape.

## Seed Data Assumptions

The local seed data is meant for product development and demos, not production accuracy.

It includes:

- 12 shelters
- mixed barrier levels and population types
- open, full, near-full, and closed shelters
- a range of intake styles
- sample guests, bookings, and turn-away records
- bookings in multiple statuses for booking workflow testing

## Testing Notes

Automated coverage currently includes:

- backend controller and service tests for shelter and booking APIs
- controller coverage for public booking request creation and validation
- a frontend smoke test confirming `/` is wired as the Spring Boot welcome page

Manual verification completed during implementation:

- public and staff frontend assets are bundled into Spring Boot static resources
- the app starts successfully with the shared frontend registered as the welcome page

Current limitation:

- direct editing of `currentOccupancy` is intentionally not exposed in the UI because the backend shelter update DTO does not support it and occupancy is derived operationally from booking lifecycle actions
- public booking requests create a new guest profile per submission in this MVP flow, so duplicate submission detection is limited

## Documentation

Additional backend model documentation:

- [docs/MVP_BACKEND_MODEL.md](/Users/camille/Shelter-Space/docs/MVP_BACKEND_MODEL.md:1)

Change history:

- [CHANGELOG.md](/Users/camille/Shelter-Space/CHANGELOG.md:1)
