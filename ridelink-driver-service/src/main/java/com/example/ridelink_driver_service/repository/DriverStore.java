package com.example.ridelink_driver_service.repository;
import com.example.ridelink_driver_service.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface DriverStore extends JpaRepository<Driver, Long> {
    List<Driver> findByAvailableTrueAndReservationIdIsNullOrderByIdAsc();
    Optional<Driver> findByAccountId(Long id);
    Optional<Driver> findByLicenseNumberIgnoreCase(String license);
}
