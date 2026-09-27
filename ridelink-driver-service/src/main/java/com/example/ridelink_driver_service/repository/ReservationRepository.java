package com.example.ridelink_driver_service.repository;
import com.example.ridelink_driver_service.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReservationRepository extends JpaRepository<Reservation, String> { }
