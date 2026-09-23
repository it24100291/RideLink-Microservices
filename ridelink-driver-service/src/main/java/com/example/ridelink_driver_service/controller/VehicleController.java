package com.example.ridelink_driver_service.controller;

import com.example.ridelink_driver_service.model.Vehicle;
import com.example.ridelink_driver_service.service.VehicleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping("/drivers/{driverId}/vehicles")
    public Vehicle createVehicle(
            @PathVariable Long driverId,
            @RequestBody Vehicle vehicle) {

        return vehicleService.createVehicle(driverId, vehicle);
    }

    @GetMapping("/drivers/{driverId}/vehicles")
    public List<Vehicle> getVehiclesByDriverId(@PathVariable Long driverId) {
        return vehicleService.getVehiclesByDriverId(driverId);
    }

    @GetMapping("/vehicles/{vehicleId}")
    public Vehicle getVehicleById(@PathVariable Long vehicleId) {
        return vehicleService.getVehicleById(vehicleId);
    }
}
