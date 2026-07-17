# Changelog

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
