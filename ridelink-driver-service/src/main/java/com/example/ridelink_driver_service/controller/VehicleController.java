package com.example.ridelink_driver_service.controller;
import com.example.ridelink_driver_service.model.Vehicle;
import com.example.ridelink_driver_service.service.*;
import com.example.ridelink_driver_service.security.Identity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api")
@Validated
public class VehicleController {
    private final VehicleService vehicles;
    private final DriverService drivers;
    public VehicleController(VehicleService vehicles, DriverService drivers) { this.vehicles=vehicles; this.drivers=drivers; }
    @PostMapping("/drivers/{driverId}/vehicles")
    public Vehicle create(@RequestAttribute("identity") Identity identity, @PathVariable @Positive Long driverId,
            @Valid @RequestBody Vehicle vehicle) {
        drivers.owned(driverId, identity);
        vehicle.setId(null);
        return vehicles.createVehicle(driverId, vehicle);
    }
    @GetMapping("/drivers/{driverId}/vehicles")
    public List<Vehicle> list(@RequestAttribute("identity") Identity identity, @PathVariable @Positive Long driverId) {
        drivers.owned(driverId, identity); return vehicles.getVehiclesByDriverId(driverId);
    }
    @GetMapping("/vehicles/{vehicleId}")
    public Vehicle get(@RequestAttribute("identity") Identity identity, @PathVariable @Positive Long vehicleId) {
        Vehicle vehicle=vehicles.getVehicleById(vehicleId);
        drivers.owned(vehicle.getDriverId(), identity); return vehicle;
    }
}
