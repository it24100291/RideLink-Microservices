package com.example.ridelink_driver_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DriverValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsDriverWithValidInput() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driverJson("Alex Driver", "DL12345")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(response.get("id").asLong() > 0);
        assertEquals("Alex Driver", response.get("name").asText());
    }

    @Test
    void rejectsDuplicateLicenseNumber() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driverJson("First Driver", "UNIQUE-123")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driverJson("Second Driver", "unique-123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A driver with this license number already exists"));
    }

    @Test
    void rejectsEmptyDriverName() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driverJson(" ", "DL12345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void rejectsMissingLicenseNumber() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alex Driver\",\"available\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.licenseNumber").exists());
    }

    @Test
    void rejectsLicenseNumberOutsideAllowedLength() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driverJson("Alex Driver", "AB")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.licenseNumber").exists());
    }

    @Test
    void rejectsNonPositiveDriverId() throws Exception {
        mockMvc.perform(patch("/api/drivers/0/availability").param("available", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returnsNotFoundForMissingDriverAndVehicle() throws Exception {
        mockMvc.perform(get("/api/drivers/999999/vehicles"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Driver not found"));

        mockMvc.perform(get("/api/vehicles/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Vehicle not found"));

        mockMvc.perform(patch("/api/drivers/999999/availability").param("available", "true"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Driver not found"));
    }

    @Test
    void acceptsValidVehicleRegistrationAndRejectsInvalidRegistration() throws Exception {
        MvcResult driverResult = mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driverJson("Vehicle Test Driver", "LIC98765")))
                .andExpect(status().isOk())
                .andReturn();
        long driverId = objectMapper.readTree(driverResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(post("/api/drivers/{driverId}/vehicles", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("CAB-1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationNumber").value("CAB-1234"));

        mockMvc.perform(post("/api/drivers/{driverId}/vehicles", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("AB")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.registrationNumber").exists());
    }

    @Test
    void preservesRideServiceAvailableDriverAndAvailabilityApiContract() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driverJson("Ride Client Driver", "RIDE123")))
                .andExpect(status().isOk())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/drivers/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].licenseNumber").value("RIDE123"));

        mockMvc.perform(patch("/api/drivers/{id}/availability", id).param("available", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.available").value(false));
    }

    private String driverJson(String name, String licenseNumber) throws Exception {
        return objectMapper.writeValueAsString(new DriverRequest(name, licenseNumber, true));
    }

    private String vehicleJson(String registrationNumber) throws Exception {
        return objectMapper.writeValueAsString(new VehicleRequest(registrationNumber, "CAR", "Toyota Prius"));
    }

    private record DriverRequest(String name, String licenseNumber, boolean available) { }
    private record VehicleRequest(String registrationNumber, String vehicleType, String model) { }
}
