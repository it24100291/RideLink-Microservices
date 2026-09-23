package com.example.ridelink_driver_service.controller;

import com.example.ridelink_driver_service.model.Vehicle;
import com.example.ridelink_driver_service.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Vehicles", description = "Vehicle registration and lookup")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping("/drivers/{driverId}/vehicles")
    @Operation(summary = "Register a vehicle", description = "Register a vehicle for an existing driver.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle registered successfully",
                    content = @Content(schema = @Schema(implementation = Vehicle.class))),
            @ApiResponse(responseCode = "400", description = "Invalid vehicle data"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    public Vehicle createVehicle(
            @Parameter(description = "Driver identifier", required = true, example = "1")
            @PathVariable Long driverId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Vehicle registration details",
                    required = true)
            @Valid @RequestBody Vehicle vehicle) {

        return vehicleService.createVehicle(driverId, vehicle);
    }

    @GetMapping("/drivers/{driverId}/vehicles")
    @Operation(summary = "Get a driver's vehicles", description = "Return all vehicles belonging to a driver.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver vehicles returned"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    @Parameter(name = "driverId", description = "Driver identifier", required = true, example = "1")
    public List<Vehicle> getVehiclesByDriverId(@PathVariable Long driverId) {
        return vehicleService.getVehiclesByDriverId(driverId);
    }

    @GetMapping("/vehicles/{vehicleId}")
    @Operation(summary = "Get a vehicle", description = "Return a vehicle by its identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle returned",
                    content = @Content(schema = @Schema(implementation = Vehicle.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    @Parameter(name = "vehicleId", description = "Vehicle identifier", required = true, example = "1")
    public Vehicle getVehicleById(@PathVariable Long vehicleId) {
        return vehicleService.getVehicleById(vehicleId);
    }
}
