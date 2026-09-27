package com.example.ridelink_driver_service.repository;
import com.example.ridelink_driver_service.model.Vehicle;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public class VehicleRepository {
    private final VehicleStore store;
    public VehicleRepository(VehicleStore store) { this.store = store; }
    public Vehicle save(Vehicle vehicle) { return store.saveAndFlush(vehicle); }
    public List<Vehicle> findAll() { return store.findAll(); }
    public List<Vehicle> findByDriverId(Long id) { return store.findByDriverId(id); }
    public Vehicle findById(Long id) { return store.findById(id).orElse(null); }
}
