package com.example.ridelink_driver_service;

import com.example.ridelink_driver_service.model.Driver;
import com.example.ridelink_driver_service.model.Vehicle;
import com.example.ridelink_driver_service.repository.DriverRepository;
import com.example.ridelink_driver_service.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DriverControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DriverService driverService;

    @Autowired
    private DriverRepository driverRepository;

    @BeforeEach
    void setUp() {
        driverRepository.deleteAll();
    }

    @Test
    void blankNameShouldReturnBadRequest() throws Exception {
        Driver invalidDriver = validDriver();
        invalidDriver.setName(" ");

        expectBadDriverRequest(invalidDriver);
    }

    @Test
    void blankLicenseNumberShouldReturnBadRequest() throws Exception {
        Driver invalidDriver = validDriver();
        invalidDriver.setLicenseNumber("");

        expectBadDriverRequest(invalidDriver);
    }

    @Test
    void blankServiceAreaShouldReturnBadRequest() throws Exception {
        Driver invalidDriver = validDriver();
        invalidDriver.setServiceArea(" ");

        expectBadDriverRequest(invalidDriver);
    }

    @Test
    void blankCurrentLocationShouldReturnBadRequest() throws Exception {
        Driver invalidDriver = validDriver();
        invalidDriver.setCurrentLocation("");

        expectBadDriverRequest(invalidDriver);
    }

    @Test
    void validDriverCreationShouldSucceed() throws Exception {
        mockMvc.perform(post("/api/drivers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDriver())))
                .andExpect(status().isOk());
    }

    @Test
    void missingDriverAvailabilityShouldReturnNotFound() throws Exception {
        mockMvc.perform(patch("/api/drivers/999999/availability")
                .param("available", "true"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingDriverLocationShouldReturnNotFound() throws Exception {
        mockMvc.perform(patch("/api/drivers/999999/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDriver())))
                .andExpect(status().isNotFound());
    }

    @Test
    void blankRegistrationNumberShouldReturnBadRequest() throws Exception {
        Driver driver = driverService.createDriver(validDriver());
        Vehicle invalidVehicle = validVehicle();
        invalidVehicle.setRegistrationNumber("");

        expectBadVehicleRequest(driver.getId(), invalidVehicle);
    }

    @Test
    void blankVehicleTypeShouldReturnBadRequest() throws Exception {
        Driver driver = driverService.createDriver(validDriver());
        Vehicle invalidVehicle = validVehicle();
        invalidVehicle.setVehicleType(" ");

        expectBadVehicleRequest(driver.getId(), invalidVehicle);
    }

    @Test
    void blankVehicleModelShouldReturnBadRequest() throws Exception {
        Driver driver = driverService.createDriver(validDriver());
        Vehicle invalidVehicle = validVehicle();
        invalidVehicle.setModel("");

        expectBadVehicleRequest(driver.getId(), invalidVehicle);
    }

    @Test
    void validVehicleCreationShouldSucceed() throws Exception {
        Driver driver = driverService.createDriver(validDriver());

        mockMvc.perform(post("/api/drivers/{driverId}/vehicles", driver.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validVehicle())))
                .andExpect(status().isOk());
    }

    @Test
    void vehicleCreationForMissingDriverShouldReturnNotFound() throws Exception {
        mockMvc.perform(post("/api/drivers/999999/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validVehicle())))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingDriverVehiclesShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/drivers/999999/vehicles"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingVehicleShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/vehicles/999999"))
                .andExpect(status().isNotFound());
    }

    private void expectBadDriverRequest(Driver driver) throws Exception {
        mockMvc.perform(post("/api/drivers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(driver)))
                .andExpect(status().isBadRequest());
    }

    private void expectBadVehicleRequest(Long driverId, Vehicle vehicle) throws Exception {
        mockMvc.perform(post("/api/drivers/{driverId}/vehicles", driverId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vehicle)))
                .andExpect(status().isBadRequest());
    }

    private Driver validDriver() {
        return new Driver(null, "Ayesha", "LIC-1234", true, "Malabe", "SLIIT Malabe");
    }

    private Vehicle validVehicle() {
        return new Vehicle(null, null, "ABC-1234", "Car", "Toyota Prius");
    }
}
