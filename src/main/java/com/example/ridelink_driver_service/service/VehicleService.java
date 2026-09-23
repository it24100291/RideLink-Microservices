package com.example.ridelink_driver_service.service;

import com.example.ridelink_driver_service.model.Driver;
import com.example.ridelink_driver_service.model.Vehicle;
import com.example.ridelink_driver_service.repository.DriverRepository;
import com.example.ridelink_driver_service.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class VehicleService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public VehicleService(DriverRepository driverRepository, VehicleRepository vehicleRepository) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public Vehicle createVehicle(Long driverId, Vehicle vehicle) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));

        vehicle.setDriverId(driver.getId());
        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getVehiclesByDriverId(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));

        return vehicleRepository.findByDriverId(driver.getId());
    }

    public Vehicle getVehicleById(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
    }
}
