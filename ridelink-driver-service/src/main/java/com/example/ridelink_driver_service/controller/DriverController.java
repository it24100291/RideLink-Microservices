
package com.example.ridelink_driver_service.controller;

import com.example.ridelink_driver_service.model.Driver;
import com.example.ridelink_driver_service.service.DriverService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@Validated
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    // Create a new driver
    @PostMapping
    public Driver createDriver(@Valid @RequestBody Driver driver) {
        return driverService.createDriver(driver);
    }

    // Retrieve available drivers
    @GetMapping("/available")
    public List<Driver> getAvailableDrivers() {
        return driverService.getAvailableDrivers();
    }

    // Update driver availability
    @PatchMapping("/{id}/availability")
    public Driver updateAvailability(
            @PathVariable @Positive Long id,
            @RequestParam boolean available) {

        return driverService.updateAvailability(id, available);
    }
}