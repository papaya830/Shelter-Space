package com.enactus.shelterspace.controller;

import com.enactus.shelterspace.model.GuestProfile;
import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.model.ShelterBooking;
import com.enactus.shelterspace.model.enums.BarrierLevel;
import com.enactus.shelterspace.model.enums.BookingChannel;
import com.enactus.shelterspace.model.enums.BookingStatus;
import com.enactus.shelterspace.model.enums.IntakeType;
import com.enactus.shelterspace.model.enums.PopulationType;
import com.enactus.shelterspace.model.enums.ShelterStatus;
import com.enactus.shelterspace.repository.GuestProfileRepository;
import com.enactus.shelterspace.repository.ShelterBookingRepository;
import com.enactus.shelterspace.repository.ShelterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.seed.enabled=false")
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShelterRepository shelterRepository;

    @Autowired
    private GuestProfileRepository guestProfileRepository;

    @Autowired
    private ShelterBookingRepository shelterBookingRepository;

    private Shelter availableShelter;
    private Shelter fullShelter;
    private GuestProfile firstGuest;
    private GuestProfile secondGuest;
    private ShelterBooking requestedBooking;
    private ShelterBooking admittedBooking;

    @BeforeEach
    void setUp() {
        shelterBookingRepository.deleteAll();
        shelterRepository.deleteAll();
        guestProfileRepository.deleteAll();

        availableShelter = shelterRepository.save(buildShelter("Available Shelter", 5, 2));
        fullShelter = shelterRepository.save(buildShelter("Full Shelter", 1, 1));
        firstGuest = guestProfileRepository.save(buildGuest("Alex", "604-555-1000"));
        secondGuest = guestProfileRepository.save(buildGuest("Sam", "604-555-1001"));

        requestedBooking = shelterBookingRepository.save(buildBooking(
                availableShelter,
                firstGuest,
                BookingStatus.REQUESTED,
                BookingChannel.PHONE,
                LocalDate.of(2026, 7, 18)
        ));

        admittedBooking = shelterBookingRepository.save(buildBooking(
                availableShelter,
                secondGuest,
                BookingStatus.ADMITTED,
                BookingChannel.STAFF_MANUAL,
                LocalDate.of(2026, 7, 18)
        ));
    }

    @Test
    void createBookingReturns201AndBookingSummary() throws Exception {
        GuestProfile thirdGuest = guestProfileRepository.save(buildGuest("Jules", "604-555-1002"));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shelterId": %d,
                                  "guestId": %d,
                                  "requestedBedDate": "2026-07-18",
                                  "requestChannel": "PHONE",
                                  "requestedBy": "Front Desk",
                                  "intakeNotes": "Needs lower bunk if available"
                                }
                                """.formatted(availableShelter.getId(), thirdGuest.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/bookings/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.guest.displayName").value("Jules"))
                .andExpect(jsonPath("$.shelter.name").value("Available Shelter"))
                .andExpect(jsonPath("$.shelter.availableBeds").value(3));
    }

    @Test
    void createBookingReturns400ForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shelterId": %d,
                                  "requestChannel": "PHONE"
                                }
                                """.formatted(availableShelter.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fields.guestId").value("Guest id is required"))
                .andExpect(jsonPath("$.fields.requestedBedDate").value("Requested bed date is required"));
    }

    @Test
    void createPublicBookingReturns201AndCreatesGuestSummary() throws Exception {
        mockMvc.perform(post("/api/bookings/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shelterId": %d,
                                  "displayName": "Casey",
                                  "legalName": "Casey Rivera",
                                  "phoneNumber": "604-555-3000",
                                  "birthDate": "1992-06-04",
                                  "requestedBedDate": "2026-07-18",
                                  "intakeNotes": "Will arrive with a backpack"
                                }
                                """.formatted(availableShelter.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/bookings/")))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.requestChannel").value("APP"))
                .andExpect(jsonPath("$.requestedBy").value("Public Web"))
                .andExpect(jsonPath("$.guest.displayName").value("Casey"))
                .andExpect(jsonPath("$.guest.phoneNumber").value("604-555-3000"))
                .andExpect(jsonPath("$.shelter.id").value(availableShelter.getId()));
    }

    @Test
    void createPublicBookingReturns400ForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/bookings/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "",
                                  "requestedBedDate": "2026-07-16"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fields.shelterId").value("Shelter id is required"))
                .andExpect(jsonPath("$.fields.displayName").value("Display name or alias is required"))
                .andExpect(jsonPath("$.fields.requestedBedDate").value("Requested bed date cannot be in the past"));
    }

    @Test
    void listBookingsReturnsStaffReviewData() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$[0].guest.displayName").value("Alex"))
                .andExpect(jsonPath("$[0].shelter.name").value("Available Shelter"))
                .andExpect(jsonPath("$[1].status").value("ADMITTED"));
    }

    @Test
    void getBookingReturnsSingleBooking() throws Exception {
        mockMvc.perform(get("/api/bookings/{id}", requestedBooking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestedBooking.getId()))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.guest.displayName").value("Alex"))
                .andExpect(jsonPath("$.shelter.id").value(availableShelter.getId()));
    }

    @Test
    void getBookingReturns404WhenMissing() throws Exception {
        mockMvc.perform(get("/api/bookings/{id}", 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Booking not found: 99999"));
    }

    @Test
    void admitBookingSucceedsAndUpdatesShelterAvailability() throws Exception {
        mockMvc.perform(post("/api/bookings/{id}/admit", requestedBooking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staffName": "Dana",
                                  "notes": "Approved for tonight"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ADMITTED"))
                .andExpect(jsonPath("$.decidedBy").value("Dana"))
                .andExpect(jsonPath("$.shelter.currentOccupancy").value(3))
                .andExpect(jsonPath("$.shelter.availableBeds").value(2));
    }

    @Test
    void admitBookingFailsWhenShelterIsFull() throws Exception {
        GuestProfile thirdGuest = guestProfileRepository.save(buildGuest("Jordan", "604-555-1003"));
        ShelterBooking fullBooking = shelterBookingRepository.save(buildBooking(
                fullShelter,
                thirdGuest,
                BookingStatus.REQUESTED,
                BookingChannel.PHONE,
                LocalDate.of(2026, 7, 18)
        ));

        mockMvc.perform(post("/api/bookings/{id}/admit", fullBooking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staffName": "Dana",
                                  "notes": "Try overflow"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Shelter has no beds available"));
    }

    @Test
    void duplicateActiveBookingReturns409() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shelterId": %d,
                                  "guestId": %d,
                                  "requestedBedDate": "2026-07-18",
                                  "requestChannel": "PHONE"
                                }
                                """.formatted(fullShelter.getId(), firstGuest.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Guest already has an active booking lifecycle"));
    }

    @Test
    void rejectBookingSucceeds() throws Exception {
        mockMvc.perform(post("/api/bookings/{id}/reject", requestedBooking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staffName": "Dana",
                                  "notes": "Does not meet tonight's intake criteria"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.decisionNotes").value("Does not meet tonight's intake criteria"));
    }

    @Test
    void waitlistBookingSucceeds() throws Exception {
        mockMvc.perform(post("/api/bookings/{id}/waitlist", requestedBooking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staffName": "Dana",
                                  "notes": "Hold for later opening"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITLISTED"))
                .andExpect(jsonPath("$.decidedBy").value("Dana"))
                .andExpect(jsonPath("$.decisionNotes").value("Hold for later opening"));
    }

    @Test
    void checkInSucceeds() throws Exception {
        mockMvc.perform(post("/api/bookings/{id}/check-in", admittedBooking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staffName": "Dana",
                                  "notes": "Guest arrived at front desk"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"))
                .andExpect(jsonPath("$.checkedInBy").value("Dana"))
                .andExpect(jsonPath("$.intakeNotes").value("Guest arrived at front desk"));
    }

    @Test
    void checkOutSucceedsAndReleasesOccupancy() throws Exception {
        admittedBooking.setStatus(BookingStatus.CHECKED_IN);
        admittedBooking.setCheckedInBy("Dana");
        admittedBooking = shelterBookingRepository.save(admittedBooking);

        mockMvc.perform(post("/api/bookings/{id}/check-out", admittedBooking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staffName": "Morgan",
                                  "notes": "Checked out in the morning"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.checkedOutBy").value("Morgan"))
                .andExpect(jsonPath("$.shelter.currentOccupancy").value(1))
                .andExpect(jsonPath("$.shelter.availableBeds").value(4));
    }

    @Test
    void invalidTransitionReturns409() throws Exception {
        mockMvc.perform(post("/api/bookings/{id}/check-in", requestedBooking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staffName": "Dana",
                                  "notes": "Attempted too early"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only admitted bookings can be checked in"));
    }

    @Test
    void cancelBookingSucceeds() throws Exception {
        mockMvc.perform(post("/api/bookings/{id}/cancel", requestedBooking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staffName": "Dana",
                                  "notes": "Guest found another placement"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.decisionNotes").value("Guest found another placement"));
    }

    private Shelter buildShelter(String name, int totalCapacity, int currentOccupancy) {
        Shelter shelter = new Shelter();
        shelter.setName(name);
        shelter.setCity("Vancouver");
        shelter.setAddress("123 Test Street");
        shelter.setPhoneNumber("604-555-0000");
        shelter.setOperationalStatus(ShelterStatus.OPEN);
        shelter.setBarrierLevel(BarrierLevel.LOW_BARRIER);
        shelter.setPopulationType(PopulationType.ANY_GENDER);
        shelter.setIntakeType(IntakeType.CALL_AHEAD);
        shelter.setTotalCapacity(totalCapacity);
        shelter.setCurrentOccupancy(currentOccupancy);
        shelter.setOpen24Hours(true);
        shelter.setCallAheadRequired(false);
        shelter.setPetsAllowed(false);
        shelter.setWheelchairAccessible(true);
        shelter.setAcceptsLargeItems(false);
        shelter.setLegalNameRequired(false);
        return shelter;
    }

    private GuestProfile buildGuest(String displayName, String phoneNumber) {
        GuestProfile guest = new GuestProfile();
        guest.setDisplayName(displayName);
        guest.setPhoneNumber(phoneNumber);
        return guest;
    }

    private ShelterBooking buildBooking(
            Shelter shelter,
            GuestProfile guest,
            BookingStatus status,
            BookingChannel channel,
            LocalDate requestedBedDate
    ) {
        ShelterBooking booking = new ShelterBooking();
        booking.setShelter(shelter);
        booking.setGuest(guest);
        booking.setStatus(status);
        booking.setRequestChannel(channel);
        booking.setRequestedBedDate(requestedBedDate);
        booking.setRequestedBy("Front Desk");
        return booking;
    }
}
