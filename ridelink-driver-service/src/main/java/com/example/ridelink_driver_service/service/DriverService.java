package com.example.ridelink_driver_service.service;

import com.example.ridelink_driver_service.model.Driver;
import com.example.ridelink_driver_service.repository.DriverRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public Driver createDriver(Driver driver) {
        return driverRepository.save(driver);
    }

    public List<Driver> getAvailableDrivers() {
        return driverRepository.findAvailable();
    }

    public Driver updateAvailability(Long id, boolean available) {
        Driver driver = driverRepository.findById(id);

        if (driver == null) {
            return null;
        }

        driver.setAvailable(available);
        return driver;
    }
}