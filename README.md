# Shelter-Space

Shelter-Space is a Spring Boot MVP for a shared shelter management and booking platform. The current backend focuses on shelter discovery, shelter operations metadata, guest profiles, booking lifecycle logic, and temporary development seed data.

## Tech Stack

- Java 17
- Spring Boot 3
- Gradle
- Spring Data JPA
- H2 in-memory database for local development

## Current MVP Backend Scope

The backend currently includes:

- shelter records and public shelter listing data
- shelter capacity and derived bed availability
- minimal guest profile data
- booking request and admission lifecycle logic with REST endpoints
- optional turn-away logging
- local seed data for development and testing

## Running Locally

### Start the app

```bash
./gradlew bootRun
```

The API starts on:

- `http://localhost:8080`

### Run tests

```bash
./gradlew test
```

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

## Seed Data Assumptions

The local seed data is meant for product development and demos, not production accuracy.

It includes:

- 12 shelters
- mixed barrier levels and population types
- open, full, near-full, and closed shelters
- a range of intake styles
- sample guests, bookings, and turn-away records
- bookings in multiple statuses for booking workflow testing

## Documentation

Additional backend model documentation:

- [docs/MVP_BACKEND_MODEL.md](/Users/camille/Shelter-Space/docs/MVP_BACKEND_MODEL.md:1)

Change history:

- [CHANGELOG.md](/Users/camille/Shelter-Space/CHANGELOG.md:1)
