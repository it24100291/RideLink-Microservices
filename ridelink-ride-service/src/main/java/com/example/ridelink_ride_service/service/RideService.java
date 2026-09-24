package com.example.ridelink_ride_service.service;

import com.example.ridelink_ride_service.client.DriverClient;
import com.example.ridelink_ride_service.model.Ride;
import com.example.ridelink_ride_service.repository.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final DriverClient driverClient;

    public RideService(RideRepository rideRepository, DriverClient driverClient) {
        this.rideRepository = rideRepository;
        this.driverClient = driverClient;
    }

    public Ride createRide(Ride ride) {
        try {
            List<DriverClient.DriverResponse> drivers = driverClient.getAvailableDrivers();
            if (drivers.isEmpty()) {
                return null;
            }

            DriverClient.DriverResponse driver = drivers.get(0);
            driverClient.updateAvailability(driver.id(), false);

            ride.setDriverId(driver.id());
            ride.setStatus("CONFIRMED");
            return rideRepository.save(ride);
        } catch (RestClientException exception) {
            return null;
        }
    }

    public Ride getRide(Long id) {
        return rideRepository.findById(id).orElse(null);
    }
}
