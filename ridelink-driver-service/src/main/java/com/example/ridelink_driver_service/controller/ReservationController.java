package com.example.ridelink_driver_service.controller;
import com.example.ridelink_driver_service.model.Reservation;
import com.example.ridelink_driver_service.service.ReservationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@RestController
@RequestMapping("/internal/reservations")
public class ReservationController {
    private final ReservationService reservations;
    private final String key;
    public ReservationController(ReservationService reservations, @Value("${integration.ride-service-key:}") String key) {
        this.reservations=reservations; this.key=key;
    }
    private void authorize(String supplied) {
        if (key.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Ride service credential is not configured");
        if (supplied == null || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid service credential");
    }
    @PutMapping("/{id}")
    public Reservation reserve(@RequestHeader(value="X-Service-Key",required=false) String supplied, @PathVariable UUID id) {
        authorize(supplied); return reservations.reserve(id.toString());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> release(@RequestHeader(value="X-Service-Key",required=false) String supplied, @PathVariable UUID id) {
        authorize(supplied); reservations.release(id.toString()); return ResponseEntity.noContent().build();
    }
}
