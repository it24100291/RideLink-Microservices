package com.example.ridelink_driver_service.controller;

import com.example.ridelink_driver_service.model.Vehicle;
import com.example.ridelink_driver_service.service.VehicleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping("/drivers/{driverId}/vehicles")
    public Vehicle createVehicle(
            @PathVariable @Positive Long driverId,
            @Valid @RequestBody Vehicle vehicle) {

        return vehicleService.createVehicle(driverId, vehicle);
    }

    @GetMapping("/drivers/{driverId}/vehicles")
    public List<Vehicle> getVehiclesByDriverId(@PathVariable @Positive Long driverId) {
        return vehicleService.getVehiclesByDriverId(driverId);
    }

    @GetMapping("/vehicles/{vehicleId}")
    public Vehicle getVehicleById(@PathVariable @Positive Long vehicleId) {
        return vehicleService.getVehicleById(vehicleId);
    }
}
