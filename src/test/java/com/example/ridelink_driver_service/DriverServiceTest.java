package com.example.ridelink_driver_service;

import com.example.ridelink_driver_service.model.Driver;
import com.example.ridelink_driver_service.repository.DriverRepository;
import com.example.ridelink_driver_service.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DriverServiceTest {

    @Autowired
    private DriverService driverService;

    @Autowired
    private DriverRepository driverRepository;

    @BeforeEach
    void setUp() {
        driverRepository.deleteAll();
    }

    @Test
    void shouldCreateDriverWithServiceAreaAndCurrentLocation() {
        Driver created = driverService.createDriver(
                new Driver(null, "Ayesha", "LIC-1234", true, "Malabe", "SLIIT Malabe")
        );

        assertNotNull(created.getId());
        assertEquals("Ayesha", created.getName());
        assertEquals("LIC-1234", created.getLicenseNumber());
        assertTrue(created.isAvailable());
        assertEquals("Malabe", created.getServiceArea());
        assertEquals("SLIIT Malabe", created.getCurrentLocation());
    }

    @Test
    void shouldUpdateDriverLocationForExistingDriver() {
        Driver driver = driverService.createDriver(
                new Driver(null, "Ayesha", "LIC-1234", true, "Malabe", "SLIIT Malabe")
        );

        Driver updated = driverService.updateLocation(driver.getId(), "Colombo", "Battaramulla");

        assertNotNull(updated);
        assertEquals("Colombo", updated.getServiceArea());
        assertEquals("Battaramulla", updated.getCurrentLocation());
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingLocationForMissingDriver() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> driverService.updateLocation(999L, "Colombo", "Malabe")
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void shouldUpdateAvailabilityAndKeepDriverBehaviorWorking() {
        Driver driver = driverService.createDriver(
                new Driver(null, "Ayesha", "LIC-1234", true, "Malabe", "SLIIT Malabe")
        );

        Driver updated = driverService.updateAvailability(driver.getId(), false);

        assertNotNull(updated);
        assertFalse(updated.isAvailable());
    }

    @Test
    void shouldReturnOnlyAvailableDrivers() {
        driverService.createDriver(new Driver(null, "Available Driver", "LIC-1", true, "Malabe", "SLIIT Malabe"));
        driverService.createDriver(new Driver(null, "Busy Driver", "LIC-2", false, "Colombo", "Fort"));

        List<Driver> availableDrivers = driverService.getAvailableDrivers();

        assertEquals(1, availableDrivers.size());
        assertTrue(availableDrivers.get(0).isAvailable());
    }

    @Test
    void shouldIncludeAvailableDriverInRequestedServiceArea() {
        driverService.createDriver(new Driver(null, "Ayesha", "LIC-1001", true, "Malabe", "SLIIT Malabe"));
        driverService.createDriver(new Driver(null, "Kamal", "LIC-1002", false, "Malabe", "Malabe Junction"));
        driverService.createDriver(new Driver(null, "Nimal", "LIC-1003", true, "Colombo", "Colombo Fort"));

        List<Driver> drivers = driverService.getAvailableDrivers("Malabe");

        assertEquals(1, drivers.size());
        assertEquals("Ayesha", drivers.get(0).getName());
    }

    @Test
    void shouldExcludeUnavailableDriverInRequestedServiceArea() {
        driverService.createDriver(new Driver(null, "Ayesha", "LIC-1001", true, "Malabe", "SLIIT Malabe"));
        driverService.createDriver(new Driver(null, "Kamal", "LIC-1002", false, "Malabe", "Malabe Junction"));

        List<Driver> drivers = driverService.getAvailableDrivers("Malabe");

        assertEquals(1, drivers.size());
        assertTrue(drivers.get(0).isAvailable());
        assertNotEquals("Kamal", drivers.get(0).getName());
    }

    @Test
    void shouldExcludeAvailableDriverOutsideRequestedServiceArea() {
        driverService.createDriver(new Driver(null, "Ayesha", "LIC-1001", true, "Malabe", "SLIIT Malabe"));
        driverService.createDriver(new Driver(null, "Nimal", "LIC-1003", true, "Colombo", "Colombo Fort"));

        List<Driver> drivers = driverService.getAvailableDrivers("Malabe");

        assertEquals(1, drivers.size());
        assertEquals("Ayesha", drivers.get(0).getName());
    }

    @Test
    void shouldMatchServiceAreaCaseInsensitively() {
        driverService.createDriver(new Driver(null, "Ayesha", "LIC-1001", true, "Malabe", "SLIIT Malabe"));

        List<Driver> drivers = driverService.getAvailableDrivers("malabe");

        assertEquals(1, drivers.size());
        assertEquals("Ayesha", drivers.get(0).getName());
    }

    @Test
    void shouldReturnEmptyListWhenNoEligibleDriversMatchServiceArea() {
        driverService.createDriver(new Driver(null, "Ayesha", "LIC-1001", true, "Malabe", "SLIIT Malabe"));

        List<Driver> drivers = driverService.getAvailableDrivers("Jaffna");

        assertNotNull(drivers);
        assertTrue(drivers.isEmpty());
    }

    @Test
    void shouldReturnAllAvailableDriversWhenServiceAreaIsNotProvided() {
        driverService.createDriver(new Driver(null, "Ayesha", "LIC-1001", true, "Malabe", "SLIIT Malabe"));
        driverService.createDriver(new Driver(null, "Nimal", "LIC-1003", true, "Colombo", "Colombo Fort"));
        driverService.createDriver(new Driver(null, "Kamal", "LIC-1002", false, "Malabe", "Malabe Junction"));

        List<Driver> drivers = driverService.getAvailableDrivers(null);

        assertEquals(2, drivers.size());
    }
}
