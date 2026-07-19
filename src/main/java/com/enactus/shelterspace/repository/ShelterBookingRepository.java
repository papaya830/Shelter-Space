package com.enactus.shelterspace.repository;

import com.enactus.shelterspace.model.ShelterBooking;
import com.enactus.shelterspace.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShelterBookingRepository extends JpaRepository<ShelterBooking, Long> {

    boolean existsByGuestIdAndStatusIn(Long guestId, Collection<BookingStatus> statuses);

    long countByShelterIdAndStatusIn(Long shelterId, Collection<BookingStatus> statuses);

    List<ShelterBooking> findByShelterIdAndRequestedBedDate(Long shelterId, LocalDate requestedBedDate);

    Optional<ShelterBooking> findTopByGuestIdOrderByRequestedAtDescIdDesc(Long guestId);

    Optional<ShelterBooking> findTopByGuestIdAndStatusInOrderByRequestedAtDescIdDesc(
            Long guestId,
            Collection<BookingStatus> statuses
    );
}
