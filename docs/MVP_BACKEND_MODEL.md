# Shelter-Space MVP Backend Model

Last updated: July 17, 2026

## Purpose

This document describes the current MVP backend model for Shelter-Space.

The goal is to support a small shared shelter booking system that is realistic enough for product and API work during the hackathon, while still staying simple enough to evolve quickly.

## Scope

The current backend supports:

- shelter records
- shelter capacity and bed availability
- shelter rules and intake instructions
- minimal guest/client identity
- booking requests
- waitlist, admit, reject, check-in, and check-out states
- optional turn-away logging
- temporary development seed data using H2

The current backend does not try to solve:

- auth and roles
- individual bed assignment
- household/family relationship modeling
- document uploads
- SMS delivery and audit
- advanced reporting
- multi-shelter referral workflows
- production-grade concurrency guarantees

## Domain Model

### 1. Static Shelter Metadata

Entity: `Shelter`

File: [src/main/java/com/enactus/shelterspace/model/Shelter.java](/Users/camille/Shelter-Space/src/main/java/com/enactus/shelterspace/model/Shelter.java:27)

Purpose:

- stores mostly stable shelter configuration and descriptive information
- includes a small amount of operational configuration needed by the MVP

Key fields:

- `name`
- `organizationName`
- `city`
- `address`
- `confidentialAddress`
- `phoneNumber`
- `operationalStatus`
- `barrierLevel`
- `populationType`
- `intakeType`
- `open24Hours`
- `callAheadRequired`
- `petsAllowed`
- `wheelchairAccessible`
- `acceptsLargeItems`
- `legalNameRequired`
- `intakeStartTime`
- `intakeCutoffTime`
- `maxStayDays`
- `minimumAge`
- `maximumAge`
- `programs`
- `rules`
- `intakeInstructions`
- `notes`
- `perks`

Availability fields kept directly on shelter:

- `totalCapacity`
- `currentOccupancy`
- derived `availableBeds`

Rationale:

- for the MVP, storing total capacity and current occupancy directly on the shelter is faster to build against than introducing a separate inventory model
- occupancy is updated through booking lifecycle actions

### 2. Guest / Client Data

Entity: `GuestProfile`

File: [src/main/java/com/enactus/shelterspace/model/GuestProfile.java](/Users/camille/Shelter-Space/src/main/java/com/enactus/shelterspace/model/GuestProfile.java:16)

Purpose:

- stores minimal identity only
- avoids assuming every guest is an app-native user

Key fields:

- `displayName` required
- `legalName` optional
- `birthDate` optional
- `phoneNumber` optional
- `notes` optional

Privacy decisions:

- no email required
- no government ID fields
- no health, legal, or case-management data
- legal name remains optional

### 3. Booking / Admission Lifecycle

Entity: `ShelterBooking`

File: [src/main/java/com/enactus/shelterspace/model/ShelterBooking.java](/Users/camille/Shelter-Space/src/main/java/com/enactus/shelterspace/model/ShelterBooking.java:23)

Relationships:

- many bookings belong to one shelter
- many bookings belong to one guest

Purpose:

- tracks the booking request and admission lifecycle from initial request through checkout

Key fields:

- `shelter`
- `guest`
- `status`
- `requestChannel`
- `requestedBedDate`
- `requestedAt`
- `requestedBy`
- `decidedAt`
- `decidedBy`
- `decisionNotes`
- `checkedInAt`
- `checkedInBy`
- `checkedOutAt`
- `checkedOutBy`
- `intakeNotes`

Lifecycle statuses:

- `REQUESTED`
- `WAITLISTED`
- `ADMITTED`
- `REJECTED`
- `CHECKED_IN`
- `CHECKED_OUT`
- `CANCELLED`

Current service rules:

- a guest cannot have multiple active booking lifecycles at once
- a closed shelter cannot accept a new booking request
- a booking can only be admitted from `REQUESTED` or `WAITLISTED`
- admission increments shelter occupancy
- checkout decrements shelter occupancy
- admission is blocked when shelter capacity is full

Core logic:

File: [src/main/java/com/enactus/shelterspace/service/BookingService.java](/Users/camille/Shelter-Space/src/main/java/com/enactus/shelterspace/service/BookingService.java:24)

### 4. Turn-Away Logging

Entity: `TurnAwayLog`

File: [src/main/java/com/enactus/shelterspace/model/TurnAwayLog.java](/Users/camille/Shelter-Space/src/main/java/com/enactus/shelterspace/model/TurnAwayLog.java:20)

Purpose:

- lightweight operational logging for cases where a guest cannot be admitted

Key fields:

- `shelter`
- `guest` optional
- `reason`
- `notes`
- `occurredAt`
- `recordedBy`

This is intentionally simple. It gives the team a place to capture refusal or capacity events without building a full incident system.

## Enum Strategy

Use enums when a field drives workflow, filtering, or validation:

- `BarrierLevel`
- `PopulationType`
- `IntakeType`
- `ShelterStatus`
- `BookingStatus`
- `BookingChannel`
- `TurnAwayReason`

Use free text when the real-world values vary a lot across shelters:

- `rules`
- `intakeInstructions`
- `programs`
- `notes`
- `perks`
- address and phone formatting

Reasoning:

- enums keep API behavior stable for UI filters and backend rules
- free text avoids premature over-modeling for shelter-specific language

## Data Separation

### Static shelter metadata

- identity, location, service population, barrier level
- intake style, accessibility, pet support, large item support
- rules, programs, perks, notes

### Dynamic operational data

- `operationalStatus`
- `totalCapacity`
- `currentOccupancy`
- derived `availableBeds`
- turn-away events

### Guest/client data

- alias or display name
- optional legal name
- optional birth date
- optional phone
- minimal notes

### Booking/admission lifecycle data

- request date and request channel
- decision status
- admit / reject / waitlist actions
- check-in / check-out timestamps
- staff attribution fields

## Seed Data

File: [src/main/java/com/enactus/shelterspace/config/SeedDataLoader.java](/Users/camille/Shelter-Space/src/main/java/com/enactus/shelterspace/config/SeedDataLoader.java:27)

The development seed data includes:

- 12 shelters
- mixed populations: men, women with children, coed, youth
- high-barrier and low-barrier shelters
- open, full, near-full, and temporarily closed examples
- different intake patterns: call-ahead, line-up, first-come-first-served, referral
- max-stay variety
- sample guests
- sample bookings in multiple states
- sample turn-away logs

The data is intended for local development and demo flows, not reporting accuracy.

## Validation and Error Handling

Files:

- [src/main/java/com/enactus/shelterspace/dto/ShelterRequest.java](/Users/camille/Shelter-Space/src/main/java/com/enactus/shelterspace/dto/ShelterRequest.java:1)
- [src/main/java/com/enactus/shelterspace/dto/BookingRequest.java](/Users/camille/Shelter-Space/src/main/java/com/enactus/shelterspace/dto/BookingRequest.java:1)
- [src/main/java/com/enactus/shelterspace/dto/BookingDecisionRequest.java](/Users/camille/Shelter-Space/src/main/java/com/enactus/shelterspace/dto/BookingDecisionRequest.java:1)
- [src/main/java/com/enactus/shelterspace/exception/GlobalExceptionHandler.java](/Users/camille/Shelter-Space/src/main/java/com/enactus/shelterspace/exception/GlobalExceptionHandler.java:1)

Current validation covers:

- required shelter input
- non-negative capacity and age bounds
- booking request requires shelter, guest, date, and channel
- booking decision requires staff name

## Tests

File: [src/test/java/com/enactus/shelterspace/service/BookingServiceTest.java](/Users/camille/Shelter-Space/src/test/java/com/enactus/shelterspace/service/BookingServiceTest.java:29)

The current unit/integration-style service tests cover:

- occupancy increases on admit
- occupancy decreases on checkout
- admitting to a full shelter is blocked
- duplicate active booking lifecycles are blocked
- requests to closed shelters are blocked

## Recommended Next Steps

1. Add booking API endpoints around `BookingService`.
2. Add a shelter list response DTO instead of returning entities directly.
3. Introduce optimistic locking or another concurrency strategy before multi-user staff workflows.
4. Add family/group modeling only when the product flow requires it.
5. Replace H2 seed assumptions with PostgreSQL or Supabase-compatible migrations before deployment.

## Deferred Design Items

These were intentionally deferred:

- `Bed` or `Room` entities
- nightly reservation windows across multiple dates
- no-show policies
- shelter-specific eligibility rules engine
- full audit trail/event sourcing
- staff accounts and permissions
- guest consent and notification preferences
- outbound SMS records
- waitlist prioritization logic
- referral network modeling

This keeps the MVP narrow while preserving a clear upgrade path.
