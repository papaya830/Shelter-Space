# Changelog

## 2026-07-21 Local Assistant, Waitlists, Demo Data, And UI Reliability

### Added

- optional local Ollama integration for grounded, open-ended shelter questions, with configurable model, host, and timeout
- natural-language routing into the deterministic chatbot booking commands
- `POST /api/bookings/public/waitlist` for direct public waitlist registration at full shelters that support waitlisting
- expanded startup demo data with 17 guest profiles, 17 bookings across every lifecycle status, and guest-linked turn-away history
- `scripts/seed-turnaways-demand.sh` for populating guest-linked turn-away records and anonymized demand signals
- `scripts/smoke-test.sh` for repeatable end-to-end lifecycle verification against a running application

### Changed

- full shelter cards are greyed out and show either a waitlist action or an unavailable-registration state
- temporarily closed shelters suppress both registration and waitlisting regardless of nominal capacity
- public filters now use current time, exclude closed shelters from `Has space`, search additional shelter fields, and support reliable clear-all behavior
- factual availability, accessibility, and pet questions use deterministic live-data retrieval instead of model-generated filtering
- Turn-aways and Shelter Settings staff layouts now use responsive, bounded workspaces with corrected spacing and overflow
- frontend dates now use the actual current date instead of a fixed demo timestamp
- route changes close stale public shelter dialogs before rendering another application area
- service-worker and asset versions are advanced with UI releases to prevent stale JavaScript and CSS

### Safety And Lifecycle Rules

- public booking registration is rejected when a shelter is full
- public waitlisting is allowed only when a shelter is full, operational, and configured with `supportsWaitlist=true`
- waitlisted guests remain eligible for staff review but cannot be admitted while capacity is full
- check-out releases occupancy; staff may then admit a requested or waitlisted guest
- closed shelters reject booking and waitlist requests at both UI and API layers

### Tests

- added controller coverage for public full-shelter conflicts, public waitlist creation, available-shelter waitlist conflicts, and closed-shelter restrictions
- added chatbot coverage for natural-language bed requests and deterministic accessibility lookup
- verified public filters, registration defaults, staff navigation, staff forms, modal cleanup, and console health in the running frontend
- verified registration, public/staff waitlists, admit, check-in, check-out, reject, cancel, queue reload, turn-away logging, analytics, and chatbot flows with the lifecycle smoke script
- verified the full Gradle test suite passes

## 2026-07-18 Operations And Discovery

### Added

- `GET /api/turn-away-logs` and `POST /api/turn-away-logs` with DTO-based responses, validation, optional guest association, and recent-history support
- staff turn-away logging screen in the bundled SPA with a quick entry form and shelter-scoped recent history
- explicit `POST /api/bookings/{id}/waitlist` endpoint and controller coverage
- public shelter filters for barrier level, population type, open now, call ahead, wheelchair access, pets, and available beds

### Changed

- updated the staff booking queue to surface `WAITLISTED` distinctly from `REQUESTED` and allow staff to move requested bookings onto the waitlist
- updated `README.md` and backend model docs with turn-away logging, waitlist behavior, and public filter documentation

### Tests

- added booking controller coverage for the explicit waitlist transition
- added controller integration coverage for turn-away log listing, shelter filtering, creation, and validation
- verified the full Gradle test suite passes with the new operations and discovery features

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
