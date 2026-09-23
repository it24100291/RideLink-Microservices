package com.example.ridelink_driver_service.service;

import com.example.ridelink_driver_service.model.Driver;
import com.example.ridelink_driver_service.repository.DriverRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
        return getAvailableDrivers(null);
    }

    public List<Driver> getAvailableDrivers(String serviceArea) {
        List<Driver> availableDrivers = driverRepository.findByAvailableTrue();

        if (serviceArea == null || serviceArea.trim().isEmpty()) {
            return availableDrivers;
        }

        String targetArea = serviceArea.trim();
        return availableDrivers.stream()
                .filter(driver -> driver.getServiceArea() != null
                        && driver.getServiceArea().equalsIgnoreCase(targetArea))
                .toList();
    }

    public Driver updateAvailability(Long id, boolean available) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));

        driver.setAvailable(available);

        return driverRepository.save(driver);
    }

    public Driver updateLocation(Long id, String serviceArea, String currentLocation) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));

        driver.setServiceArea(serviceArea);
        driver.setCurrentLocation(currentLocation);

        return driverRepository.save(driver);
    }
}