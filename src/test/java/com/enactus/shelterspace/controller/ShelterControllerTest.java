package com.enactus.shelterspace.controller;

import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.model.enums.BarrierLevel;
import com.enactus.shelterspace.model.enums.IntakeType;
import com.enactus.shelterspace.model.enums.PopulationType;
import com.enactus.shelterspace.model.enums.ShelterStatus;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.seed.enabled=false")
class ShelterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShelterRepository shelterRepository;

    @Autowired
    private ShelterBookingRepository shelterBookingRepository;

    private Shelter firstShelter;
    private Shelter secondShelter;

    @BeforeEach
    void setUp() {
        shelterBookingRepository.deleteAll();
        shelterRepository.deleteAll();
        firstShelter = shelterRepository.save(buildShelter("Beacon Shelter", "Vancouver", 20, 8));
        secondShelter = shelterRepository.save(buildShelter("Gateway Shelter", "Surrey", 40, 40));
    }

    @Test
    void getAllSheltersReturnsListingFields() throws Exception {
        mockMvc.perform(get("/api/shelters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].city").value("Surrey"))
                .andExpect(jsonPath("$[0].name").value("Gateway Shelter"))
                .andExpect(jsonPath("$[0].availableBeds").value(0))
                .andExpect(jsonPath("$[1].name").value("Beacon Shelter"))
                .andExpect(jsonPath("$[1].availableBeds").value(12));
    }

    @Test
    void getShelterByIdReturnsShelter() throws Exception {
        mockMvc.perform(get("/api/shelters/{id}", firstShelter.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstShelter.getId()))
                .andExpect(jsonPath("$.name").value("Beacon Shelter"))
                .andExpect(jsonPath("$.operationalStatus").value("OPEN"))
                .andExpect(jsonPath("$.currentOccupancy").value(8))
                .andExpect(jsonPath("$.availableBeds").value(12));
    }

    @Test
    void getShelterByIdReturns404WhenMissing() throws Exception {
        mockMvc.perform(get("/api/shelters/{id}", 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Shelter not found: 99999"))
                .andExpect(jsonPath("$.path").value("/api/shelters/99999"));
    }

    @Test
    void createShelterReturns201AndLocation() throws Exception {
        mockMvc.perform(post("/api/shelters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Springhouse",
                                  "organizationName": "Spring Housing Society",
                                  "city": "Vancouver",
                                  "address": "Address confidential",
                                  "confidentialAddress": true,
                                  "phoneNumber": "604-606-0412",
                                  "operationalStatus": "OPEN",
                                  "barrierLevel": "LOW_BARRIER",
                                  "populationType": "WOMEN_WITH_CHILDREN",
                                  "intakeType": "FIRST_COME_FIRST_SERVED",
                                  "totalCapacity": 12,
                                  "open24Hours": true,
                                  "callAheadRequired": true,
                                  "petsAllowed": true,
                                  "wheelchairAccessible": true,
                                  "acceptsLargeItems": false,
                                  "legalNameRequired": false,
                                  "maxStayDays": 60,
                                  "minimumAge": 19,
                                  "programs": "Family support",
                                  "rules": "Follow staff direction",
                                  "intakeInstructions": "Call first for screening",
                                  "notes": "Confidential location",
                                  "perks": "Meals, showers"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/shelters/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Springhouse"))
                .andExpect(jsonPath("$.currentOccupancy").value(0))
                .andExpect(jsonPath("$.availableBeds").value(12))
                .andExpect(jsonPath("$.callAheadRequired").value(true));
    }

    @Test
    void updateShelterReturnsUpdatedShelter() throws Exception {
        mockMvc.perform(put("/api/shelters/{id}", firstShelter.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Beacon Shelter Updated",
                                  "organizationName": "Beacon Housing",
                                  "city": "Vancouver",
                                  "address": "123 Updated Street",
                                  "confidentialAddress": false,
                                  "phoneNumber": "604-555-1212",
                                  "operationalStatus": "LIMITED",
                                  "barrierLevel": "HIGH_BARRIER",
                                  "populationType": "MEN_ONLY",
                                  "intakeType": "CALL_AHEAD",
                                  "totalCapacity": 20,
                                  "open24Hours": true,
                                  "callAheadRequired": true,
                                  "petsAllowed": false,
                                  "wheelchairAccessible": true,
                                  "acceptsLargeItems": true,
                                  "legalNameRequired": true,
                                  "intakeStartTime": "08:00:00",
                                  "intakeCutoffTime": "22:00:00",
                                  "maxStayDays": 30,
                                  "minimumAge": 19,
                                  "maximumAge": 65,
                                  "programs": "Case management",
                                  "rules": "Quiet hours after 10 p.m.",
                                  "intakeInstructions": "Call before arrival",
                                  "notes": "Updated note",
                                  "perks": "Meals"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstShelter.getId()))
                .andExpect(jsonPath("$.name").value("Beacon Shelter Updated"))
                .andExpect(jsonPath("$.operationalStatus").value("LIMITED"))
                .andExpect(jsonPath("$.currentOccupancy").value(8))
                .andExpect(jsonPath("$.availableBeds").value(12))
                .andExpect(jsonPath("$.legalNameRequired").value(true));
    }

    @Test
    void updateShelterReturns404WhenMissing() throws Exception {
        mockMvc.perform(put("/api/shelters/{id}", 99999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validShelterPayload()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Shelter not found: 99999"));
    }

    @Test
    void deleteShelterReturns204() throws Exception {
        mockMvc.perform(delete("/api/shelters/{id}", firstShelter.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/shelters/{id}", firstShelter.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteShelterReturns404WhenMissing() throws Exception {
        mockMvc.perform(delete("/api/shelters/{id}", 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Shelter not found: 99999"));
    }

    @Test
    void createShelterReturns400ForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/shelters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "city": "",
                                  "address": "",
                                  "operationalStatus": "OPEN",
                                  "barrierLevel": "LOW_BARRIER",
                                  "populationType": "ANY_GENDER",
                                  "intakeType": "CALL_AHEAD",
                                  "totalCapacity": -1,
                                  "minimumAge": 30,
                                  "maximumAge": 18
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/shelters"))
                .andExpect(jsonPath("$.fields.name").value("Shelter name is required"))
                .andExpect(jsonPath("$.fields.city").value("City is required"))
                .andExpect(jsonPath("$.fields.address").value("Address is required"))
                .andExpect(jsonPath("$.fields.totalCapacity").value("Capacity cannot be negative"))
                .andExpect(jsonPath("$.fields.ageRangeValid").value("Maximum age must be greater than or equal to minimum age"));
    }

    @Test
    void updateShelterReturns409WhenCapacityFallsBelowCurrentOccupancy() throws Exception {
        mockMvc.perform(put("/api/shelters/{id}", firstShelter.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Beacon Shelter",
                                  "organizationName": "Beacon Housing",
                                  "city": "Vancouver",
                                  "address": "123 Test Street",
                                  "confidentialAddress": false,
                                  "phoneNumber": "604-555-1212",
                                  "operationalStatus": "OPEN",
                                  "barrierLevel": "LOW_BARRIER",
                                  "populationType": "ANY_GENDER",
                                  "intakeType": "CALL_AHEAD",
                                  "totalCapacity": 5,
                                  "open24Hours": true,
                                  "callAheadRequired": false,
                                  "petsAllowed": false,
                                  "wheelchairAccessible": true,
                                  "acceptsLargeItems": false,
                                  "legalNameRequired": false
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Capacity cannot be lower than current occupancy"));
    }

    private Shelter buildShelter(String name, String city, int totalCapacity, int currentOccupancy) {
        Shelter shelter = new Shelter();
        shelter.setName(name);
        shelter.setOrganizationName(name + " Org");
        shelter.setCity(city);
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
        shelter.setPetsAllowed(true);
        shelter.setWheelchairAccessible(true);
        shelter.setAcceptsLargeItems(false);
        shelter.setLegalNameRequired(false);
        shelter.setIntakeInstructions("Call ahead.");
        shelter.setPrograms("Meals");
        shelter.setRules("Respect quiet hours.");
        shelter.setPerks("Showers");
        return shelter;
    }

    private String validShelterPayload() {
        return """
                {
                  "name": "Updated Shelter",
                  "organizationName": "Updated Org",
                  "city": "Vancouver",
                  "address": "123 Test Street",
                  "confidentialAddress": false,
                  "phoneNumber": "604-555-1111",
                  "operationalStatus": "OPEN",
                  "barrierLevel": "LOW_BARRIER",
                  "populationType": "ANY_GENDER",
                  "intakeType": "CALL_AHEAD",
                  "totalCapacity": 10,
                  "open24Hours": true,
                  "callAheadRequired": false,
                  "petsAllowed": false,
                  "wheelchairAccessible": true,
                  "acceptsLargeItems": false,
                  "legalNameRequired": false
                }
                """;
    }
}
