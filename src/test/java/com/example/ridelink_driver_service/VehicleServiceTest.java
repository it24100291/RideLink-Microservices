package com.example.ridelink_driver_service;

import com.example.ridelink_driver_service.model.Driver;
import com.example.ridelink_driver_service.model.Vehicle;
import com.example.ridelink_driver_service.repository.DriverRepository;
import com.example.ridelink_driver_service.service.DriverService;
import com.example.ridelink_driver_service.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VehicleServiceTest {

    @Autowired
    private DriverService driverService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private DriverRepository driverRepository;

    @BeforeEach
    void setUp() {
        driverRepository.deleteAll();
    }

    @Test
    void shouldCreateVehicleForExistingDriver() {
        Driver driver = driverService.createDriver(new Driver(null, "Ayesha", "LIC-1001", true, "Malabe", "SLIIT Malabe"));

        Vehicle created = vehicleService.createVehicle(driver.getId(), new Vehicle(null, driver.getId(), "ABC-1234", "Car", "Toyota Prius"));

        assertNotNull(created.getId());
        assertEquals(driver.getId(), created.getDriverId());
        assertEquals("ABC-1234", created.getRegistrationNumber());
        assertEquals("Car", created.getVehicleType());
        assertEquals("Toyota Prius", created.getModel());
    }

    @Test
    void shouldReturnVehiclesByDriverId() {
        Driver driver = driverService.createDriver(new Driver(null, "Ayesha", "LIC-1001", true, "Malabe", "SLIIT Malabe"));
        vehicleService.createVehicle(driver.getId(), new Vehicle(null, driver.getId(), "ABC-1234", "Car", "Toyota Prius"));

        List<Vehicle> vehicles = vehicleService.getVehiclesByDriverId(driver.getId());

        assertEquals(1, vehicles.size());
        assertEquals(driver.getId(), vehicles.get(0).getDriverId());
    }

    @Test
    void shouldGetVehicleById() {
        Driver driver = driverService.createDriver(new Driver(null, "Ayesha", "LIC-1001", true, "Malabe", "SLIIT Malabe"));
        Vehicle vehicle = vehicleService.createVehicle(driver.getId(), new Vehicle(null, driver.getId(), "ABC-1234", "Car", "Toyota Prius"));

        Vehicle found = vehicleService.getVehicleById(vehicle.getId());

        assertNotNull(found);
        assertEquals(vehicle.getId(), found.getId());
    }

    @Test
    void shouldRejectVehicleForNonexistentDriver() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> vehicleService.createVehicle(999L, new Vehicle(null, 999L, "ABC-1234", "Car", "Toyota Prius"))
        );

        assertEquals(404, exception.getStatusCode().value());
    }
}
