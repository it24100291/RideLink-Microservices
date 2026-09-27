package com.example.ridelink_ride_service.repository;
import com.example.ridelink_ride_service.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RideRepository extends JpaRepository<Ride,Long> {
    Optional<Ride> findByReservationId(String id);
    List<Ride> findByPassengerAccountIdOrderByIdDesc(Long id);
    List<Ride> findByDriverAccountIdOrderByIdDesc(Long id);
    List<Ride> findByReleasePendingTrueOrStatus(String status);
}
