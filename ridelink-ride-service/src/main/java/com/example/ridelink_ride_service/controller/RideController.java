package com.example.ridelink_ride_service.controller;

import com.example.ridelink_ride_service.model.Ride;
import com.example.ridelink_ride_service.service.RideService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    public ResponseEntity<?> createRide(@RequestBody Ride ride) {

        Ride createdRide = rideService.createRide(ride);

        if (createdRide == null) {
            return ResponseEntity
                    .status(503)
                    .body(java.util.Map.of(
                            "code", "DRIVER_SERVICE_UNAVAILABLE",
                            "message", "A driver cannot be assigned now. Please retry."
                    ));
        }
        return ResponseEntity.ok(createdRide);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRide(@PathVariable Long id) {

        Ride ride = rideService.getRide(id);

        if (ride == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ride);
    }
}