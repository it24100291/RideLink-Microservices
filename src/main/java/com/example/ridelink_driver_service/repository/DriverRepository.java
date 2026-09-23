package com.example.ridelink_driver_service.repository;

import com.example.ridelink_driver_service.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findByAvailableTrue();
}