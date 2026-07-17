package com.enactus.shelterspace.service;

import com.enactus.shelterspace.dto.BookingDecisionRequest;
import com.enactus.shelterspace.dto.BookingRequest;
import com.enactus.shelterspace.dto.BookingResponse;
import com.enactus.shelterspace.exception.BookingConflictException;
import com.enactus.shelterspace.model.GuestProfile;
import com.enactus.shelterspace.model.Shelter;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = "app.seed.enabled=false")
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ShelterRepository shelterRepository;

    @Autowired
    private GuestProfileRepository guestProfileRepository;

    @Autowired
    private ShelterBookingRepository shelterBookingRepository;

    private Shelter shelter;
    private GuestProfile firstGuest;
    private GuestProfile secondGuest;

    @BeforeEach
    void setUp() {
        shelterBookingRepository.deleteAll();
        shelterRepository.deleteAll();
        guestProfileRepository.deleteAll();

        shelter = new Shelter();
        shelter.setName("Test Shelter");
        shelter.setCity("Vancouver");
        shelter.setAddress("123 Test Street");
        shelter.setPhoneNumber("604-555-0000");
        shelter.setOperationalStatus(ShelterStatus.OPEN);
        shelter.setBarrierLevel(BarrierLevel.LOW_BARRIER);
        shelter.setPopulationType(PopulationType.ANY_GENDER);
        shelter.setIntakeType(IntakeType.CALL_AHEAD);
        shelter.setTotalCapacity(1);
        shelter.setCurrentOccupancy(0);
        shelter.setOpen24Hours(true);
        shelter.setCallAheadRequired(false);
        shelter.setPetsAllowed(false);
        shelter.setWheelchairAccessible(true);
        shelter.setAcceptsLargeItems(false);
        shelter.setLegalNameRequired(false);
        shelter = shelterRepository.save(shelter);

        firstGuest = new GuestProfile();
        firstGuest.setDisplayName("Alex");
        firstGuest = guestProfileRepository.save(firstGuest);

        secondGuest = new GuestProfile();
        secondGuest.setDisplayName("Sam");
        secondGuest = guestProfileRepository.save(secondGuest);
    }

    @Test
    void admitConsumesCapacityAndCheckOutRestoresIt() {
        BookingResponse booking = bookingService.createRequest(buildRequest(firstGuest.getId()));

        BookingResponse admitted = bookingService.admit(booking.id(), buildDecision("Staff One"));
        assertThat(admitted.status()).isEqualTo(BookingStatus.ADMITTED);
        assertThat(shelterRepository.findById(shelter.getId()).orElseThrow().getCurrentOccupancy()).isEqualTo(1);

        BookingResponse checkedOut = bookingService.checkOut(booking.id(), buildDecision("Staff Two"));
        assertThat(checkedOut.status()).isEqualTo(BookingStatus.CHECKED_OUT);
        assertThat(shelterRepository.findById(shelter.getId()).orElseThrow().getCurrentOccupancy()).isEqualTo(0);
    }

    @Test
    void admitFailsWhenShelterIsFull() {
        BookingResponse firstBooking = bookingService.createRequest(buildRequest(firstGuest.getId()));
        bookingService.admit(firstBooking.id(), buildDecision("Staff One"));

        BookingResponse secondBooking = bookingService.createRequest(buildRequest(secondGuest.getId()));

        assertThatThrownBy(() -> bookingService.admit(secondBooking.id(), buildDecision("Staff Two")))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("no beds available");
    }

    @Test
    void guestCannotHaveTwoActiveBookingsAtOnce() {
        bookingService.createRequest(buildRequest(firstGuest.getId()));

        assertThatThrownBy(() -> bookingService.createRequest(buildRequest(firstGuest.getId())))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("active booking lifecycle");
    }

    @Test
    void requestFailsWhenShelterIsClosed() {
        shelter.setOperationalStatus(ShelterStatus.TEMPORARILY_CLOSED);
        shelterRepository.save(shelter);

        assertThatThrownBy(() -> bookingService.createRequest(buildRequest(firstGuest.getId())))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("closed shelter");
    }

    @Test
    void checkInFailsWhenBookingWasNotAdmitted() {
        BookingResponse booking = bookingService.createRequest(buildRequest(firstGuest.getId()));

        assertThatThrownBy(() -> bookingService.checkIn(booking.id(), buildDecision("Staff One")))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("Only admitted bookings can be checked in");
    }

    @Test
    void cancelMovesRequestedBookingToCancelled() {
        BookingResponse booking = bookingService.createRequest(buildRequest(firstGuest.getId()));

        BookingResponse cancelled = bookingService.cancel(booking.id(), buildDecision("Staff One"));

        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelled.decidedBy()).isEqualTo("Staff One");
    }

    private BookingRequest buildRequest(Long guestId) {
        BookingRequest request = new BookingRequest();
        request.setShelterId(shelter.getId());
        request.setGuestId(guestId);
        request.setRequestedBedDate(LocalDate.of(2026, 7, 18));
        request.setRequestChannel(BookingChannel.PHONE);
        request.setRequestedBy("Test Intake");
        return request;
    }

    private BookingDecisionRequest buildDecision(String staffName) {
        BookingDecisionRequest request = new BookingDecisionRequest();
        request.setStaffName(staffName);
        request.setNotes("Test note");
        return request;
    }
}
