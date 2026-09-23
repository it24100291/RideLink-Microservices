package com.example.ridelink_driver_service.controller;

import com.example.ridelink_driver_service.model.Driver;
import com.example.ridelink_driver_service.service.DriverService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@Tag(name = "Drivers", description = "Driver registration, availability, service areas, and simulated locations")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
        @Operation(summary = "Create a driver", description = "Create and register a driver.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver created successfully",
                content = @Content(schema = @Schema(implementation = Driver.class))),
            @ApiResponse(responseCode = "400", description = "Invalid driver data")
        })
    public Driver createDriver(@Valid @RequestBody Driver driver) {
        return driverService.createDriver(driver);
    }

    @GetMapping("/available")
        @Operation(summary = "Get available drivers", description = "Return only available drivers. When serviceArea is supplied, results are limited to that area using case-insensitive matching.")
        @ApiResponse(responseCode = "200", description = "Available drivers returned")
            public List<Driver> getAvailableDrivers(
                @Parameter(description = "Optional service area filter; matching is case-insensitive", example = "Malabe")
                @RequestParam(required = false) String serviceArea) {
        return driverService.getAvailableDrivers(serviceArea);
    }

    @PatchMapping("/{id}/availability")
        @Operation(summary = "Update driver availability", description = "Update whether a driver is available for service.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver availability updated",
                content = @Content(schema = @Schema(implementation = Driver.class))),
            @ApiResponse(responseCode = "400", description = "Invalid availability request")
        })
    public Driver updateAvailability(
            @Parameter(description = "Driver identifier", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Whether the driver is available", required = true, example = "true")
            @RequestParam boolean available) {

        return driverService.updateAvailability(id, available);
    }

    @PatchMapping("/{id}/location")
        @Operation(summary = "Update driver location", description = "Update the driver's simulated current location and service area.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver location updated",
                content = @Content(schema = @Schema(implementation = Driver.class))),
            @ApiResponse(responseCode = "400", description = "Invalid location request"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
        })
    public Driver updateLocation(
            @Parameter(description = "Driver identifier", required = true, example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated service area and simulated current location",
                required = true)
            @Valid @RequestBody Driver locationUpdate) {

        return driverService.updateLocation(id, locationUpdate.getServiceArea(), locationUpdate.getCurrentLocation());
    }
}