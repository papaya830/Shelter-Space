package com.enactus.shelterspace.model;

import com.enactus.shelterspace.model.enums.BookingChannel;
import com.enactus.shelterspace.model.enums.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shelter_bookings")
@Getter
@Setter
@NoArgsConstructor
public class ShelterBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelter_id", nullable = false)
    private Shelter shelter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false)
    private GuestProfile guest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BookingStatus status = BookingStatus.REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BookingChannel requestChannel = BookingChannel.STAFF_MANUAL;

    @Column(nullable = false)
    private LocalDate requestedBedDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime decidedAt;

    private LocalDateTime checkedInAt;

    private LocalDateTime checkedOutAt;

    @Column(length = 120)
    private String requestedBy;

    @Column(length = 120)
    private String decidedBy;

    @Column(length = 120)
    private String checkedInBy;

    @Column(length = 120)
    private String checkedOutBy;

    @Column(length = 1000)
    private String decisionNotes;

    @Column(length = 1000)
    private String intakeNotes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (requestedAt == null) {
            requestedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
