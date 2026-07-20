package com.enactus.shelterspace.config;

import com.enactus.shelterspace.model.GuestProfile;
import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.model.ShelterBooking;
import com.enactus.shelterspace.model.TurnAwayLog;
import com.enactus.shelterspace.model.enums.BarrierLevel;
import com.enactus.shelterspace.model.enums.BookingChannel;
import com.enactus.shelterspace.model.enums.BookingStatus;
import com.enactus.shelterspace.model.enums.IntakeType;
import com.enactus.shelterspace.model.enums.PopulationType;
import com.enactus.shelterspace.model.enums.ShelterStatus;
import com.enactus.shelterspace.model.enums.TurnAwayReason;
import com.enactus.shelterspace.repository.GuestProfileRepository;
import com.enactus.shelterspace.repository.ShelterBookingRepository;
import com.enactus.shelterspace.repository.ShelterRepository;
import com.enactus.shelterspace.repository.TurnAwayLogRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Configuration
public class SeedDataLoader {

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner loadSeedData(
            ShelterRepository shelterRepository,
            GuestProfileRepository guestProfileRepository,
            ShelterBookingRepository shelterBookingRepository,
            TurnAwayLogRepository turnAwayLogRepository
    ) {
        return args -> {
            if (shelterRepository.count() > 0) {
                return;
            }

            Shelter gatewayHope = buildShelter(
                    "Gateway Of Hope - Emergency Shelter", "Langley", "5787 Langley Bypass", "604-514-7375",
                    ShelterStatus.OPEN, BarrierLevel.LOW_BARRIER, PopulationType.ANY_GENDER, IntakeType.CALL_AHEAD,
                    42, 34, false, true, true, false, true, true,
                    null, null, 30, 19, null,
                    "Emergency beds, meals, case management",
                    "Zero tolerance for violence; quiet hours after 10 p.m.",
                    "Call front desk before arrival to confirm same-night availability.",
                    "Pet friendly and wheelchair accessible.",
                    "Meals, showers, pet support",
                    49.0957, -122.6576
            );
            gatewayHope.setSupportsWaitlist(true);
            shelterRepository.save(gatewayHope);

            Shelter catholicMens = buildShelter(
                    "Catholic Charities Men's Shelter", "Vancouver", "1056 Comox Street", "604-443-3292",
                    ShelterStatus.OPEN, BarrierLevel.HIGH_BARRIER, PopulationType.MEN_ONLY, IntakeType.CALL_AHEAD,
                    32, 32, false, false, false, false, true, false,
                    LocalTime.of(8, 0), LocalTime.of(21, 0), 30, 19, null,
                    "Emergency shelter, meals",
                    "Sobriety expected during stay.",
                    "Call ahead for screening; government ID preferred but not required.",
                    "Frequently full by evening.",
                    "Meals",
                    49.2858, -123.1289
            );
            catholicMens.setSupportsWaitlist(true);
            shelterRepository.save(catholicMens);

            Shelter northShore = buildShelter(
                    "North Shore Housing Centre Shelter", "North Vancouver", "705 2nd Street West", "604-982-9126 Ext 0",
                    ShelterStatus.OPEN, BarrierLevel.LOW_BARRIER, PopulationType.ANY_GENDER, IntakeType.CALL_AHEAD,
                    28, 27, false, false, false, false, true, false,
                    LocalTime.of(9, 0), LocalTime.of(23, 0), 14, 19, null,
                    "Emergency beds, outreach",
                    "Respect staff directions and shared sleeping areas.",
                    "Phone intake recommended before arrival.",
                    "Often keeps one or two spaces open for urgent placements.",
                    "Showers",
                    49.3195, -123.0724
            );
            northShore.setSupportsWaitlist(true);
            shelterRepository.save(northShore);

            Shelter stevensonHouse = buildShelter(
                    "Stevenson House For Men", "New Westminster", "32 Elliot Street", "604-526-4783",
                    ShelterStatus.OPEN, BarrierLevel.HIGH_BARRIER, PopulationType.MEN_ONLY, IntakeType.CALL_AHEAD,
                    20, 20, false, false, false, false, true, false,
                    LocalTime.of(8, 0), LocalTime.of(22, 0), 21, 19, null,
                    "Overnight shelter",
                    "Guests must return before curfew.",
                    "No intakes after 10 p.m.",
                    "Matches the live note from the source list.",
                    "Laundry",
                    49.2058, -122.9120
            );
            stevensonHouse.setSupportsWaitlist(true);
            shelterRepository.save(stevensonHouse);

            Shelter rockBay = buildShelter(
                    "Rock Bay Landing Emergency Shelter", "Victoria", "535 Ellice Street", "250-383-1951 Ext 1",
                    ShelterStatus.OPEN, BarrierLevel.LOW_BARRIER, PopulationType.ANY_GENDER, IntakeType.FIRST_COME_FIRST_SERVED,
                    50, 49, false, true, true, true, true, false,
                    null, null, 7, 19, null,
                    "Emergency beds, storage, meals",
                    "First-come-first-served intake each evening.",
                    "Walk up during intake hours; staff assess space at the door.",
                    "Pet friendly and takes large items.",
                    "Storage, pet support, meals",
                    48.4284, -123.3656
            );
            rockBay.setSupportsWaitlist(true);
            shelterRepository.save(rockBay);

            Shelter springhouse = shelterRepository.save(buildShelter(
                    "Springhouse", "Vancouver", "Address confidential", "604-606-0412",
                    ShelterStatus.OPEN, BarrierLevel.LOW_BARRIER, PopulationType.WOMEN_WITH_CHILDREN, IntakeType.FIRST_COME_FIRST_SERVED,
                    12, 10, true, true, true, false, true, false,
                    null, null, 60, 19, null,
                    "Pregnancy and family support",
                    "Women and family-focused communal living guidelines apply.",
                    "Confidential address; call first for screening and directions.",
                    "One stream reserved for pregnant women.",
                    "Family rooms, meals",
                    49.2827, -123.1207
            ));

            Shelter centreHope = shelterRepository.save(buildShelter(
                    "Centre Of Hope - Abbotsford Shelter", "Abbotsford", "34081 Gladys Avenue", "604-852-9305 Ext 3",
                    ShelterStatus.OPEN, BarrierLevel.LOW_BARRIER, PopulationType.ANY_GENDER, IntakeType.CALL_AHEAD,
                    45, 40, false, true, true, true, true, false,
                    null, null, 30, 19, null,
                    "Emergency beds, storage, meals",
                    "Guests must participate in basic intake on arrival.",
                    "Phone ahead for bed confirmation when possible.",
                    "Several open beds in seed data.",
                    "Meals, storage",
                    49.0490, -122.3081
            ));

            Shelter rosewood = buildShelter(
                    "Rosewood Shelter - E. Fry Society", "Surrey", "Address confidential", "604-589-5337",
                    ShelterStatus.OPEN, BarrierLevel.HIGH_BARRIER, PopulationType.WOMEN_WITH_CHILDREN, IntakeType.REFERRAL,
                    16, 14, true, false, false, false, true, false,
                    LocalTime.of(9, 0), LocalTime.of(20, 0), 45, 19, null,
                    "Family units, transition planning",
                    "Referral-based placement for women with children.",
                    "Address shared after screening or referral acceptance.",
                    "Confidential location.",
                    "Family rooms",
                    49.1913, -122.8490
            );
            rosewood.setSupportsWaitlist(true);
            shelterRepository.save(rosewood);

            Shelter theHaven = shelterRepository.save(buildShelter(
                    "The Haven", "Vancouver", "108 East Hastings Street", "604-646-6806",
                    ShelterStatus.OPEN, BarrierLevel.HIGH_BARRIER, PopulationType.MEN_ONLY, IntakeType.LINE_UP,
                    35, 29, false, false, false, false, false, false,
                    LocalTime.of(18, 0), LocalTime.of(23, 30), 14, 19, null,
                    "Emergency shelter",
                    "Behaviour-based access decisions may be made at the door.",
                    "No holds over phone. Guests are assessed at the door.",
                    "Matches shelter-list note.",
                    "Meals",
                    49.2827, -123.0970
            ));

            Shelter beacon = shelterRepository.save(buildShelter(
                    "The Beacon", "Vancouver", "108 East Hastings Street", "604-646-6846",
                    ShelterStatus.OPEN, BarrierLevel.HIGH_BARRIER, PopulationType.MEN_ONLY, IntakeType.CALL_AHEAD,
                    24, 22, false, false, false, false, false, false,
                    LocalTime.of(8, 0), LocalTime.of(23, 30), 14, 19, null,
                    "Emergency beds",
                    "Curfew at 10:30 p.m.",
                    "24-hour site; intake 8:00 a.m. to 11:30 p.m.",
                    "Matches shelter-list note.",
                    "Meals",
                    49.2827, -123.0970
            ));

            Shelter youthSafeHouse = shelterRepository.save(buildShelter(
                    "North Shore Youth Safe House", "North Vancouver", "Address confidential", "604-924-8005",
                    ShelterStatus.OPEN, BarrierLevel.LOW_BARRIER, PopulationType.YOUTH_ONLY, IntakeType.CALL_AHEAD,
                    8, 5, true, false, false, false, true, false,
                    null, null, 21, 13, 18,
                    "Youth beds, safety planning",
                    "Youth-focused programming and staff supervision.",
                    "Call for confidential intake and directions.",
                    "Any gender youth beds.",
                    "Youth support",
                    49.3195, -123.0724
            ));

            Shelter havenInHollow = shelterRepository.save(buildShelter(
                    "Haven In The Hollow", "Mission", "32646 Logan Avenue", "604-820-9008",
                    ShelterStatus.TEMPORARILY_CLOSED, BarrierLevel.LOW_BARRIER, PopulationType.ANY_GENDER, IntakeType.CALL_AHEAD,
                    25, 0, false, false, false, false, true, false,
                    null, null, 14, 19, null,
                    "Emergency shelter",
                    "Temporarily closed.",
                    "No current intake.",
                    "Seeded as closed based on the source list.",
                    "None",
                    49.1523, -122.2979
            ));

            GuestProfile alex = guestProfileRepository.save(buildGuest("Alex", null, LocalDate.of(1993, 4, 12), "604-555-0101", "Prefers alias only."));
            GuestProfile maria = guestProfileRepository.save(buildGuest("Maria R.", "Maria Ramirez", LocalDate.of(1989, 9, 3), "604-555-0102", "Travelling with one child."));
            GuestProfile devon = guestProfileRepository.save(buildGuest("Devon", null, null, null, "No legal name provided."));
            GuestProfile jules = guestProfileRepository.save(buildGuest("Jules", null, LocalDate.of(2006, 11, 1), "604-555-0104", "Youth guest."));
            GuestProfile sam = guestProfileRepository.save(buildGuest("Sam K.", "Samuel Khan", LocalDate.of(1981, 2, 14), "604-555-0105", null));

            shelterBookingRepository.save(buildBooking(gatewayHope, alex, BookingStatus.CHECKED_IN, BookingChannel.PHONE,
                    LocalDate.now().plusDays(1), "Intake Desk", "Dana", "Dana", null,
                    LocalDateTime.now().minusHours(4), LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(2), null,
                    "Late arrival okay"));

            shelterBookingRepository.save(buildBooking(rosewood, maria, BookingStatus.ADMITTED, BookingChannel.STAFF_MANUAL,
                    LocalDate.now().plusDays(1), "Referral Coordinator", "Kim", null, null,
                    LocalDateTime.now().minusHours(6), LocalDateTime.now().minusHours(5), null, null,
                    "Family unit held until 9 p.m."));

            shelterBookingRepository.save(buildBooking(northShore, devon, BookingStatus.WAITLISTED, BookingChannel.WALK_IN,
                    LocalDate.now().plusDays(1), "Front Desk", "Taylor", null, null,
                    LocalDateTime.now().minusHours(1), LocalDateTime.now().minusMinutes(30), null, null,
                    "Next spot if no-show"));

            shelterBookingRepository.save(buildBooking(youthSafeHouse, jules, BookingStatus.REQUESTED, BookingChannel.PHONE,
                    LocalDate.now().plusDays(1), "Crisis Line", null, null, null,
                    LocalDateTime.now().minusMinutes(20), null, null, null,
                    "Needs confidential placement"));

            shelterBookingRepository.save(buildBooking(centreHope, sam, BookingStatus.CHECKED_OUT, BookingChannel.APP,
                    LocalDate.now(), "Self-service", "Avery", "Avery", "Avery",
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1).plusHours(1),
                    LocalDateTime.now().minusDays(1).plusHours(2), LocalDateTime.now().minusHours(1),
                    "Stayed one night"));

            turnAwayLogRepository.save(buildTurnAway(stevensonHouse, null, TurnAwayReason.INTAKE_CLOSED,
                    "Arrived after 10 p.m. cutoff.", "Night Staff"));
            turnAwayLogRepository.save(buildTurnAway(rockBay, devon, TurnAwayReason.NO_BEDS_AVAILABLE,
                    "No overflow space left by 11 p.m.", "Front Desk"));
            turnAwayLogRepository.save(buildTurnAway(theHaven, null, TurnAwayReason.BEHAVIOUR_RESTRICTION,
                    "Door assessment did not clear intake.", "Intake Lead"));
        };
    }

    private Shelter buildShelter(
            String name,
            String city,
            String address,
            String phone,
            ShelterStatus status,
            BarrierLevel barrierLevel,
            PopulationType populationType,
            IntakeType intakeType,
            int totalCapacity,
            int currentOccupancy,
            boolean confidentialAddress,
            boolean petsAllowed,
            boolean wheelchairAccessible,
            boolean acceptsLargeItems,
            boolean open24Hours,
            boolean callAheadRequired,
            LocalTime intakeStartTime,
            LocalTime intakeCutoffTime,
            Integer maxStayDays,
            Integer minimumAge,
            Integer maximumAge,
            String programs,
            String rules,
            String intakeInstructions,
            String notes,
            String perks,
            Double latitude,
            Double longitude
    ) {
        Shelter shelter = new Shelter();
        shelter.setName(name);
        shelter.setCity(city);
        shelter.setAddress(address);
        shelter.setPhoneNumber(phone);
        shelter.setOperationalStatus(status);
        shelter.setBarrierLevel(barrierLevel);
        shelter.setPopulationType(populationType);
        shelter.setIntakeType(intakeType);
        shelter.setTotalCapacity(totalCapacity);
        shelter.setCurrentOccupancy(currentOccupancy);
        shelter.setConfidentialAddress(confidentialAddress);
        shelter.setPetsAllowed(petsAllowed);
        shelter.setWheelchairAccessible(wheelchairAccessible);
        shelter.setAcceptsLargeItems(acceptsLargeItems);
        shelter.setOpen24Hours(open24Hours);
        shelter.setCallAheadRequired(callAheadRequired);
        shelter.setIntakeStartTime(intakeStartTime);
        shelter.setIntakeCutoffTime(intakeCutoffTime);
        shelter.setMaxStayDays(maxStayDays);
        shelter.setMinimumAge(minimumAge);
        shelter.setMaximumAge(maximumAge);
        shelter.setPrograms(programs);
        shelter.setRules(rules);
        shelter.setIntakeInstructions(intakeInstructions);
        shelter.setNotes(notes);
        shelter.setPerks(perks);
        shelter.setLegalNameRequired(false);
        shelter.setLatitude(latitude);
        shelter.setLongitude(longitude);
        return shelter;
    }

    private GuestProfile buildGuest(String displayName, String legalName, LocalDate birthDate, String phoneNumber, String notes) {
        GuestProfile guest = new GuestProfile();
        guest.setDisplayName(displayName);
        guest.setLegalName(legalName);
        guest.setBirthDate(birthDate);
        guest.setPhoneNumber(phoneNumber);
        guest.setNotes(notes);
        return guest;
    }

    private ShelterBooking buildBooking(
            Shelter shelter,
            GuestProfile guest,
            BookingStatus status,
            BookingChannel channel,
            LocalDate requestedBedDate,
            String requestedBy,
            String decidedBy,
            String checkedInBy,
            String checkedOutBy,
            LocalDateTime requestedAt,
            LocalDateTime decidedAt,
            LocalDateTime checkedInAt,
            LocalDateTime checkedOutAt,
            String notes
    ) {
        ShelterBooking booking = new ShelterBooking();
        booking.setShelter(shelter);
        booking.setGuest(guest);
        booking.setStatus(status);
        booking.setRequestChannel(channel);
        booking.setRequestedBedDate(requestedBedDate);
        booking.setRequestedBy(requestedBy);
        booking.setDecidedBy(decidedBy);
        booking.setCheckedInBy(checkedInBy);
        booking.setCheckedOutBy(checkedOutBy);
        booking.setRequestedAt(requestedAt);
        booking.setDecidedAt(decidedAt);
        booking.setCheckedInAt(checkedInAt);
        booking.setCheckedOutAt(checkedOutAt);
        booking.setDecisionNotes(notes);
        booking.setIntakeNotes(notes);
        return booking;
    }

    private TurnAwayLog buildTurnAway(Shelter shelter, GuestProfile guest, TurnAwayReason reason, String notes, String recordedBy) {
        TurnAwayLog turnAwayLog = new TurnAwayLog();
        turnAwayLog.setShelter(shelter);
        turnAwayLog.setGuest(guest);
        turnAwayLog.setReason(reason);
        turnAwayLog.setNotes(notes);
        turnAwayLog.setRecordedBy(recordedBy);
        turnAwayLog.setOccurredAt(LocalDateTime.now().minusHours(2));
        return turnAwayLog;
    }
}
