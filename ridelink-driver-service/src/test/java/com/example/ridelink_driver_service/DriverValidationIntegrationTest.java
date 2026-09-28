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

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.example.ridelink_driver_service.security.AccountClient accounts;

    @Autowired private com.example.ridelink_driver_service.repository.DriverStore drivers;
    @Autowired private com.example.ridelink_driver_service.repository.VehicleStore vehicles;

    @org.junit.jupiter.api.BeforeEach
    void setupIdentity() {
        vehicles.deleteAll(); drivers.deleteAll();
        org.mockito.Mockito.when(accounts.authenticate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.example.ridelink_driver_service.security.Identity(10L, "Test", "DRIVER"));
    }

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
                .andExpect(jsonPath("$[?(@.id == " + id + ")].name").value("Ride Client Driver"));

        mockMvc.perform(patch("/api/drivers/{id}/availability", id).param("available", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void returnsAllAvailableDriversWhenServiceAreaIsOmitted() throws Exception {
        saveDriver("Malabe Driver", "MALABE1", true, "Malabe");
        saveDriver("Colombo Driver", "COLOMBO1", true, "Colombo");
        saveDriver("Unavailable Malabe Driver", "MALABE2", false, "Malabe");

        mockMvc.perform(get("/api/drivers/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..name").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "Malabe Driver", "Colombo Driver")));
    }

    @Test
    void filtersAvailableDriversByServiceAreaIgnoringCase() throws Exception {
        saveDriver("Malabe Driver", "MALABE1", true, "Malabe");
        saveDriver("Colombo Driver", "COLOMBO1", true, "Colombo");
        saveDriver("Unavailable Malabe Driver", "MALABE2", false, "Malabe");

        mockMvc.perform(get("/api/drivers/available").param("serviceArea", "mAlAbE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..name").value(org.hamcrest.Matchers.contains("Malabe Driver")));
    }

    @Test
    void returnsEmptyListWhenNoAvailableDriversMatchServiceArea() throws Exception {
        saveDriver("Malabe Driver", "MALABE1", true, "Malabe");

        mockMvc.perform(get("/api/drivers/available").param("serviceArea", "Jaffna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void updatesExistingDriverLocation() throws Exception {
        com.example.ridelink_driver_service.model.Driver driver =
                new com.example.ridelink_driver_service.model.Driver(null, "Location Driver", "LOC12345", true);
        driver.setAccountId(10L);
        driver = drivers.save(driver);

        mockMvc.perform(patch("/api/drivers/{id}/location", driver.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentLocation\":\"Colombo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentLocation").value("Colombo"));
    }

    @Test
    void returnsNotFoundWhenUpdatingLocationForMissingDriver() throws Exception {
        mockMvc.perform(patch("/api/drivers/999999/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentLocation\":\"Colombo\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Driver not found"));
    }

    @Test
    void listsAndRetrievesVehiclesForTheirDriver() throws Exception {
        com.example.ridelink_driver_service.model.Driver driver =
                new com.example.ridelink_driver_service.model.Driver(null, "Vehicle Driver", "VEH12345", true);
        driver.setAccountId(10L);
        driver = drivers.save(driver);

        MvcResult created = mockMvc.perform(post("/api/drivers/{driverId}/vehicles", driver.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("CAB-1234")))
                .andExpect(status().isOk())
                .andReturn();
        long vehicleId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/drivers/{driverId}/vehicles", driver.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(vehicleId))
                .andExpect(jsonPath("$[0].registrationNumber").value("CAB-1234"));

        mockMvc.perform(get("/api/vehicles/{vehicleId}", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vehicleId))
                .andExpect(jsonPath("$.driverId").value(driver.getId()));
    }

    @Test
    void rejectsBlankVehicleFieldsWithStructuredValidationErrors() throws Exception {
        MvcResult driverResult = mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driverJson("Vehicle Validation Driver", "VEHVALID1")))
                .andExpect(status().isOk())
                .andReturn();
        long driverId = objectMapper.readTree(driverResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(post("/api/drivers/{driverId}/vehicles", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("", "CAR", "Toyota")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.registrationNumber").exists());

        mockMvc.perform(post("/api/drivers/{driverId}/vehicles", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("CAB-1234", "", "Toyota")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.vehicleType").exists());

        mockMvc.perform(post("/api/drivers/{driverId}/vehicles", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("CAB-1234", "CAR", " ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.model").exists());
    }

    @Test
    void rejectsBlankLicenseNumber() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alex Driver\",\"licenseNumber\":\" \",\"available\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.licenseNumber").exists());
    }

    @Test
    void allowsBlankOptionalServiceAreaAndCurrentLocation() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Optional Fields Driver\",\"licenseNumber\":\"OPTIONAL1\","
                                + "\"available\":true,\"serviceArea\":\" \",\"currentLocation\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceArea").value(" "))
                .andExpect(jsonPath("$.currentLocation").value(""));
    }

    private void saveDriver(String name, String license, boolean available, String serviceArea) {
        com.example.ridelink_driver_service.model.Driver driver =
                new com.example.ridelink_driver_service.model.Driver(null, name, license, available);
        driver.setServiceArea(serviceArea);
        drivers.save(driver);
    }

    private String driverJson(String name, String licenseNumber) throws Exception {
        return objectMapper.writeValueAsString(new DriverRequest(name, licenseNumber, true));
    }

    private String vehicleJson(String registrationNumber) throws Exception {
        return vehicleJson(registrationNumber, "CAR", "Toyota Prius");
    }

    private String vehicleJson(String registrationNumber, String vehicleType, String model) throws Exception {
        return objectMapper.writeValueAsString(new VehicleRequest(registrationNumber, vehicleType, model));
    }

    private record DriverRequest(String name, String licenseNumber, boolean available) { }
    private record VehicleRequest(String registrationNumber, String vehicleType, String model) { }
}
