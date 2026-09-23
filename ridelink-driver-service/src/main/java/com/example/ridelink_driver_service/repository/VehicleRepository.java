package com.example.ridelink_driver_service.repository;

import com.example.ridelink_driver_service.model.Vehicle;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class VehicleRepository {

    private final List<Vehicle> vehicles = new ArrayList<>();

    public Vehicle save(Vehicle vehicle) {
        vehicle.setId((long) (vehicles.size() + 1));
        vehicles.add(vehicle);
        return vehicle;
    }

    public List<Vehicle> findAll() {
        return vehicles;
    }

    public List<Vehicle> findByDriverId(Long driverId) {
        return vehicles.stream()
                .filter(vehicle -> vehicle.getDriverId().equals(driverId))
                .toList();
    }

    public Vehicle findById(Long id) {
        return vehicles.stream()
                .filter(vehicle -> vehicle.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
