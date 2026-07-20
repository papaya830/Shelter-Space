package com.enactus.shelterspace.model;

import com.enactus.shelterspace.model.enums.BarrierLevel;
import com.enactus.shelterspace.model.enums.IntakeType;
import com.enactus.shelterspace.model.enums.PopulationType;
import com.enactus.shelterspace.model.enums.ShelterStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "shelters")
@Getter
@Setter
@NoArgsConstructor
public class Shelter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String organizationName;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false)
    private boolean confidentialAddress;

    @Column(length = 50)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ShelterStatus operationalStatus = ShelterStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BarrierLevel barrierLevel = BarrierLevel.LOW_BARRIER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PopulationType populationType = PopulationType.ANY_GENDER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IntakeType intakeType = IntakeType.CALL_AHEAD;

    @Column(nullable = false)
    @Min(0)
    private Integer totalCapacity;

    @Column(nullable = false)
    @Min(0)
    private Integer currentOccupancy = 0;

    @Column(nullable = false)
    private boolean open24Hours = true;

    @Column(nullable = false)
    private boolean callAheadRequired;

    @Column(nullable = false)
    private boolean petsAllowed;

    @Column(nullable = false)
    private boolean wheelchairAccessible;

    @Column(nullable = false)
    private boolean acceptsLargeItems;

    @Column(nullable = false)
    private boolean legalNameRequired;

    @Column(nullable = false)
    private boolean supportsWaitlist = false;

    private LocalTime intakeStartTime;

    private LocalTime intakeCutoffTime;

    @Min(0)
    private Integer maxStayDays;

    @Min(0)
    @Max(130)
    private Integer minimumAge;

    @Min(0)
    @Max(130)
    private Integer maximumAge;

    @Column(length = 1000)
    private String programs;

    @Column(length = 2000)
    private String rules;

    @Column(length = 2000)
    private String intakeInstructions;

    @Column(length = 2000)
    private String notes;

    @Column(length = 1000)
    private String perks;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    public int getAvailableBeds() {
        return Math.max(0, totalCapacity - currentOccupancy);
    }

    public boolean hasCapacity() {
        return currentOccupancy < totalCapacity;
    }

    public void incrementOccupancy() {
        if (!hasCapacity()) {
            throw new IllegalStateException("Shelter is at capacity");
        }
        currentOccupancy += 1;
    }

    public void decrementOccupancy() {
        if (currentOccupancy > 0) {
            currentOccupancy -= 1;
        }
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
