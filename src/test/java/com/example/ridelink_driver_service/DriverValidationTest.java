package com.example.ridelink_driver_service;

import com.example.ridelink_driver_service.model.Driver;
import com.example.ridelink_driver_service.repository.DriverRepository;
import com.example.ridelink_driver_service.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DriverValidationTest {

    @Autowired
    private DriverService driverService;

    @Autowired
    private DriverRepository driverRepository;

    @BeforeEach
    void setUp() {
        driverRepository.deleteAll();
    }

    @Test
    void validDriverCreationSucceeds() {
        Driver created = driverService.createDriver(new Driver(null, "Ayesha", "LIC-1234", true, "Malabe", "SLIIT Malabe"));

        assertNotNull(created);
        assertEquals("Ayesha", created.getName());
        assertEquals("LIC-1234", created.getLicenseNumber());
        assertEquals("Malabe", created.getServiceArea());
        assertEquals("SLIIT Malabe", created.getCurrentLocation());
    }

    @Test
    void blankDriverNameIsRejected() {
        Driver driver = new Driver(null, "", "LIC-1234", true, "Malabe", "SLIIT Malabe");
        assertThrows(Exception.class, () -> driverService.createDriver(driver));
    }

    @Test
    void blankLicenseNumberIsRejected() {
        Driver driver = new Driver(null, "Ayesha", "", true, "Malabe", "SLIIT Malabe");
        assertThrows(Exception.class, () -> driverService.createDriver(driver));
    }

    @Test
    void blankServiceAreaIsRejected() {
        Driver driver = new Driver(null, "Ayesha", "LIC-1234", true, "", "SLIIT Malabe");
        assertThrows(Exception.class, () -> driverService.createDriver(driver));
    }

    @Test
    void blankCurrentLocationIsRejected() {
        Driver driver = new Driver(null, "Ayesha", "LIC-1234", true, "Malabe", "");
        assertThrows(Exception.class, () -> driverService.createDriver(driver));
    }
}
