package com.example.ridelink_driver_service.service;
import com.example.ridelink_driver_service.model.*;
import com.example.ridelink_driver_service.repository.*;
import com.example.ridelink_driver_service.security.Identity;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class DriverService {
    private final DriverRepository drivers;
    public DriverService(DriverRepository drivers) { this.drivers = drivers; }
    public synchronized Driver createDriver(Driver driver) {
        if (drivers.findByLicenseNumber(driver.getLicenseNumber().trim()) != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A driver with this license number already exists");
        if (driver.getAccountId() != null && drivers.findByAccountId(driver.getAccountId()) != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This account already has a driver profile");
        driver.setId(null);
        driver.setLicenseNumber(driver.getLicenseNumber().trim());
        return drivers.save(driver);
    }
    public List<Driver> getAvailableDrivers() { return drivers.findAvailable(); }
    public List<Driver> getAvailableDrivers(String serviceArea) {
        if (serviceArea == null || serviceArea.isBlank()) return getAvailableDrivers();
        return drivers.findAvailable(serviceArea.trim());
    }
    public Driver get(Long id) {
        Driver driver = drivers.findById(id);
        if (driver == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found");
        return driver;
    }
    public Driver owned(Long id, Identity identity) {
        identity.require("DRIVER");
        Driver driver = get(id);
        if (!identity.accountId().equals(driver.getAccountId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This driver profile belongs to another account");
        return driver;
    }
    public Driver mine(Identity identity) {
        identity.require("DRIVER");
        Driver driver = drivers.findByAccountId(identity.accountId());
        if (driver == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found");
        return driver;
    }
    public synchronized Driver updateAvailability(Long id, boolean available) {
        Driver driver = get(id);
        if (driver.getReservationId() != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Driver has an active ride reservation");
        driver.setAvailable(available);
        return drivers.save(driver);
    }
    public synchronized Driver updateLocation(Long id, String location) {
        Driver driver = get(id);
        driver.setCurrentLocation(location.trim());
        return drivers.save(driver);
    }
}
