package com.enactus.shelterspace.controller;

import com.enactus.shelterspace.model.GuestProfile;
import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.model.TurnAwayLog;
import com.enactus.shelterspace.model.enums.BarrierLevel;
import com.enactus.shelterspace.model.enums.IntakeType;
import com.enactus.shelterspace.model.enums.PopulationType;
import com.enactus.shelterspace.model.enums.ShelterStatus;
import com.enactus.shelterspace.model.enums.TurnAwayReason;
import com.enactus.shelterspace.repository.GuestProfileRepository;
import com.enactus.shelterspace.repository.ShelterRepository;
import com.enactus.shelterspace.repository.TurnAwayLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

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
class TurnAwayLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShelterRepository shelterRepository;

    @Autowired
    private GuestProfileRepository guestProfileRepository;

    @Autowired
    private TurnAwayLogRepository turnAwayLogRepository;

    private Shelter firstShelter;
    private Shelter secondShelter;
    private GuestProfile guest;

    @BeforeEach
    void setUp() {
        turnAwayLogRepository.deleteAll();
        shelterRepository.deleteAll();
        guestProfileRepository.deleteAll();

        firstShelter = shelterRepository.save(buildShelter("North Shelter", "Vancouver"));
        secondShelter = shelterRepository.save(buildShelter("South Shelter", "Burnaby"));

        guest = new GuestProfile();
        guest.setDisplayName("Jordan");
        guest.setPhoneNumber("604-555-0101");
        guest = guestProfileRepository.save(guest);

        turnAwayLogRepository.save(buildTurnAway(firstShelter, guest, TurnAwayReason.NO_BEDS_AVAILABLE, LocalDateTime.of(2026, 7, 18, 18, 15)));
        turnAwayLogRepository.save(buildTurnAway(secondShelter, null, TurnAwayReason.INTAKE_CLOSED, LocalDateTime.of(2026, 7, 18, 17, 45)));
    }

    @Test
    void listTurnAwayLogsReturnsRecentLogs() throws Exception {
        mockMvc.perform(get("/api/turn-away-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].reason").value("NO_BEDS_AVAILABLE"))
                .andExpect(jsonPath("$[0].guest.displayName").value("Jordan"))
                .andExpect(jsonPath("$[1].reason").value("INTAKE_CLOSED"));
    }

    @Test
    void listTurnAwayLogsCanFilterByShelter() throws Exception {
        mockMvc.perform(get("/api/turn-away-logs").param("shelterId", String.valueOf(firstShelter.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].shelter.id").value(firstShelter.getId()))
                .andExpect(jsonPath("$[0].reason").value("NO_BEDS_AVAILABLE"));
    }

    @Test
    void createTurnAwayLogReturns201AndStructuredResponse() throws Exception {
        mockMvc.perform(post("/api/turn-away-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shelterId": %d,
                                  "guestId": %d,
                                  "reason": "REFERRED_ELSEWHERE",
                                  "notes": "Sent to overflow site",
                                  "occurredAt": "2026-07-18T19:30:00",
                                  "recordedBy": "Dana"
                                }
                                """.formatted(firstShelter.getId(), guest.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/turn-away-logs/")))
                .andExpect(jsonPath("$.reason").value("REFERRED_ELSEWHERE"))
                .andExpect(jsonPath("$.recordedBy").value("Dana"))
                .andExpect(jsonPath("$.guest.id").value(guest.getId()))
                .andExpect(jsonPath("$.shelter.id").value(firstShelter.getId()));
    }

    @Test
    void createTurnAwayLogReturns400ForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/turn-away-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notes": "No staff name or shelter"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fields.shelterId").value("Shelter id is required"))
                .andExpect(jsonPath("$.fields.reason").value("Turn-away reason is required"))
                .andExpect(jsonPath("$.fields.recordedBy").value("Recorded by is required"));
    }

    private Shelter buildShelter(String name, String city) {
        Shelter shelter = new Shelter();
        shelter.setName(name);
        shelter.setCity(city);
        shelter.setAddress("123 Test Street");
        shelter.setPhoneNumber("604-555-0000");
        shelter.setOperationalStatus(ShelterStatus.OPEN);
        shelter.setBarrierLevel(BarrierLevel.LOW_BARRIER);
        shelter.setPopulationType(PopulationType.ANY_GENDER);
        shelter.setIntakeType(IntakeType.CALL_AHEAD);
        shelter.setTotalCapacity(10);
        shelter.setCurrentOccupancy(6);
        shelter.setOpen24Hours(true);
        shelter.setCallAheadRequired(false);
        shelter.setPetsAllowed(false);
        shelter.setWheelchairAccessible(true);
        shelter.setAcceptsLargeItems(false);
        shelter.setLegalNameRequired(false);
        return shelter;
    }

    private TurnAwayLog buildTurnAway(Shelter shelter, GuestProfile guestProfile, TurnAwayReason reason, LocalDateTime occurredAt) {
        TurnAwayLog turnAwayLog = new TurnAwayLog();
        turnAwayLog.setShelter(shelter);
        turnAwayLog.setGuest(guestProfile);
        turnAwayLog.setReason(reason);
        turnAwayLog.setNotes("Sample log");
        turnAwayLog.setOccurredAt(occurredAt);
        turnAwayLog.setRecordedBy("Morgan");
        return turnAwayLog;
    }
}
