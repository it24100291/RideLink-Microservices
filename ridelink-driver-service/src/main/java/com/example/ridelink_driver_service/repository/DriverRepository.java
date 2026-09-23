package com.example.ridelink_driver_service.repository;

import com.example.ridelink_driver_service.model.Driver;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class DriverRepository {

    private final List<Driver> drivers = new ArrayList<>();

    public Driver save(Driver driver) {
        driver.setId((long) (drivers.size() + 1));
        drivers.add(driver);
        return driver;
    }

    public List<Driver> findAll() {
        return drivers;
    }

    public List<Driver> findAvailable() {
        return drivers.stream()
                .filter(Driver::isAvailable)
                .toList();
    }

    public Driver findById(Long id) {
        return drivers.stream()
                .filter(driver -> driver.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}