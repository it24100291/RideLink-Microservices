package com.example.ridelink_driver_service.repository;
import com.example.ridelink_driver_service.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface VehicleStore extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByDriverId(Long id);
}
