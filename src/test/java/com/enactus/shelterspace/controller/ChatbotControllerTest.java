package com.enactus.shelterspace.controller;

import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.model.ShelterBooking;
import com.enactus.shelterspace.model.enums.BarrierLevel;
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

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.seed.enabled=false")
class ChatbotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShelterRepository shelterRepository;

    @Autowired
    private ShelterBookingRepository shelterBookingRepository;

    @Autowired
    private GuestProfileRepository guestProfileRepository;

    @BeforeEach
    void setUp() {
        shelterBookingRepository.deleteAll();
        shelterRepository.deleteAll();
        guestProfileRepository.deleteAll();

        shelterRepository.save(buildShelter("Union Gospel", 8, 6, BarrierLevel.LOW_BARRIER, PopulationType.MEN_ONLY));
        shelterRepository.save(buildShelter("Lookout", 12, 11, BarrierLevel.HIGH_BARRIER, PopulationType.ANY_GENDER));
        shelterRepository.save(buildShelter("Overflow House", 6, 6, BarrierLevel.LOW_BARRIER, PopulationType.ANY_GENDER));
    }

    @Test
    void helpCommandReturnsKnownKeywordsAndSegmentSafeResponses() throws Exception {
        mockMvc.perform(post("/api/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientSessionId": "chat-test-help",
                                  "message": "help"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.messages[0]", containsString("Commands: BED, STATUS, CANCEL, MORE, DIR, HELP")))
                .andExpect(jsonPath("$.nextInputs", hasSize(greaterThan(0))));
    }

    @Test
    void chatbotFlowCreatesBookingAndStatusTracksStaffAdmit() throws Exception {
        String sessionId = "chat-flow-1";
        String alias = "Jordan";

        mockMvc.perform(post("/api/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientSessionId": "%s",
                                  "alias": "%s",
                                  "message": "BED"
                                }
                                """.formatted(sessionId, alias)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CHOOSING"))
                .andExpect(jsonPath("$.messages[0]", containsString("Shelters with space")))
                .andExpect(jsonPath("$.nextInputs[0]").value("1-2"));

        mockMvc.perform(post("/api/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientSessionId": "%s",
                                  "message": "1"
                                }
                                """.formatted(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("DURATION"));

        mockMvc.perform(post("/api/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientSessionId": "%s",
                                  "message": "2"
                                }
                                """.formatted(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CONFIRM"))
                .andExpect(jsonPath("$.messages[0]", containsString("YES or NO")));

        mockMvc.perform(post("/api/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientSessionId": "%s",
                                  "message": "YES"
                                }
                                """.formatted(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("WAITING"))
                .andExpect(jsonPath("$.bookingId").isNumber())
                .andExpect(jsonPath("$.messages[0]", containsString("Requested. Code A")));

        ShelterBooking createdBooking = shelterBookingRepository.findAll().stream()
                .max(Comparator.comparing(ShelterBooking::getId))
                .orElseThrow();

        mockMvc.perform(post("/api/bookings/{id}/admit", createdBooking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staffName": "Staff Demo",
                                  "notes": "Admitted from queue"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientSessionId": "%s",
                                  "message": "STATUS"
                                }
                                """.formatted(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0]", containsString("Status: ADMITTED")))
                .andExpect(jsonPath("$.messages[0]", containsString("Reply DIR for directions")));
    }

    private Shelter buildShelter(
            String name,
            int totalCapacity,
            int currentOccupancy,
            BarrierLevel barrierLevel,
            PopulationType populationType
    ) {
        Shelter shelter = new Shelter();
        shelter.setName(name);
        shelter.setCity("Vancouver");
        shelter.setAddress("123 Demo Street");
        shelter.setPhoneNumber("604-555-0000");
        shelter.setOperationalStatus(ShelterStatus.OPEN);
        shelter.setBarrierLevel(barrierLevel);
        shelter.setPopulationType(populationType);
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
}
