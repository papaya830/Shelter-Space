# Changelog

## 2026-07-17 Public Frontend

### Added

- public shelter browsing flow with list, detail, and booking request views inside the bundled Spring Boot SPA
- `POST /api/bookings/public` for privacy-conscious public booking requests that create a guest profile and booking in one step
- controller coverage for public booking request success and validation failures

### Changed

- split the bundled SPA into public and staff route groups so the public UI is the default landing experience and the staff console remains available at `/#/staff/dashboard`
- updated `README.md` with public UI access, booking flow, API integration notes, and MVP limitations

### Tests

- verified the full Gradle test suite passes after the public booking endpoint and SPA routing changes

## 2026-07-17 Staff Frontend

### Added

- bundled Spring Boot staff frontend served from `/` with booking queue, shelter availability, and shelter config screens
- booking action workflow for admit, reject, check-in, and check-out using the live booking API
- shelter edit form with inline validation mapped from backend error responses
- frontend smoke test verifying the welcome-page route is wired to the bundled UI

### Changed

- updated `README.md` with staff frontend usage, local run flow, frontend/API integration notes, and current MVP limitations
- kept shelter occupancy read-only in the frontend because the current shelter update API does not support direct occupancy edits

### Tests

- verified the full Gradle test suite passes with the bundled frontend assets included

## 2026-07-17

### Added

- MVP shelter domain model with `Shelter`, `GuestProfile`, `ShelterBooking`, and `TurnAwayLog`
- supporting enums for shelter operations, intake style, booking lifecycle, and turn-away reasons
- `BookingService` with core booking lifecycle rules
- repositories for guest profiles, bookings, and turn-away logs
- development seed loader with 12 realistic shelters, sample guests, bookings, and turn-away events
- booking request and decision DTOs
- booking conflict exception handling
- backend model documentation in `docs/MVP_BACKEND_MODEL.md`

### Changed

- completed the previously scaffolded shelter CRUD service and controller
- expanded `ShelterRequest` validation to match the new shelter model
- updated H2/JPA configuration for clean seeded development startup
- improved global API error responses for validation, not-found, and booking conflicts

### Tests

- added booking service coverage for occupancy tracking
- added booking service coverage for preventing over-capacity admission
- added booking service coverage for preventing duplicate active bookings
- added booking service coverage for blocking requests to closed shelters

## 2026-07-17 Shelter API Polish

### Added

- `ShelterResponse` DTO for stable shelter API responses with derived `availableBeds`
- `README.md` with local setup, shelter endpoints, and H2 usage
- controller integration tests covering shelter CRUD, validation, conflict, and not-found behavior

### Changed

- updated shelter controller and service to return response DTOs instead of JPA entities
- added `Location` header behavior for shelter creation
- improved shelter request validation with age-range and intake-window checks
- made shelter listing responses deterministic with sorting by city then name
- improved error responses with request `path` and clearer bad-request handling

### Tests

- added `ShelterControllerTest` with endpoint-level assertions for `GET`, `POST`, `PUT`, and `DELETE`

## 2026-07-17 Booking API

### Added

- `BookingController` exposing booking creation, retrieval, admit, reject, check-in, check-out, and cancel endpoints
- DTO-based booking responses with nested guest and shelter summaries
- controller integration tests for booking lifecycle endpoints and API failures

### Changed

- updated `BookingService` to return DTO responses instead of entities
- trimmed booking request and decision note inputs before persistence
- added a cancellable booking transition for requested and waitlisted bookings
- documented booking endpoints and workflow in `README.md` and `docs/MVP_BACKEND_MODEL.md`

### Tests

- expanded booking service coverage for invalid check-in transition and cancel flow
- added `BookingControllerTest` with endpoint-level assertions for create, list, detail, admit, reject, check-in, check-out, cancel, validation, conflict, and 404 behavior
