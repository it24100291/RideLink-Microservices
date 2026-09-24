package com.example.ridelink_driver_service.service;

import com.example.ridelink_driver_service.model.Driver;
import com.example.ridelink_driver_service.repository.DriverRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public Driver createDriver(Driver driver) {
        if (driverRepository.findByLicenseNumber(driver.getLicenseNumber()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A driver with this license number already exists");
        }
        return driverRepository.save(driver);
    }

    public List<Driver> getAvailableDrivers() {
        return driverRepository.findAvailable();
    }

    public Driver updateAvailability(Long id, boolean available) {
        Driver driver = driverRepository.findById(id);

        if (driver == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found");
        }

        driver.setAvailable(available);
        return driver;
    }
}
