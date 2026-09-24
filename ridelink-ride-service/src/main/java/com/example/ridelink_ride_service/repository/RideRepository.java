package com.example.ridelink_ride_service.repository;

import com.example.ridelink_ride_service.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideRepository extends JpaRepository<Ride, Long> {
}