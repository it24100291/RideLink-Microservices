package com.example.ridelink_driver_service.repository;
import com.example.ridelink_driver_service.model.Driver;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public class DriverRepository {
    private final DriverStore store;
    public DriverRepository(DriverStore store) { this.store = store; }
    public Driver save(Driver driver) { return store.saveAndFlush(driver); }
    public List<Driver> findAll() { return store.findAll(); }
    public List<Driver> findAvailable() { return store.findByAvailableTrueAndReservationIdIsNullOrderByIdAsc(); }
    public Driver findById(Long id) { return store.findById(id).orElse(null); }
    public Driver findByLicenseNumber(String license) { return store.findByLicenseNumberIgnoreCase(license).orElse(null); }
    public Driver findByAccountId(Long id) { return store.findByAccountId(id).orElse(null); }
}
