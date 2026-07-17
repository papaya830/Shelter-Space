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
