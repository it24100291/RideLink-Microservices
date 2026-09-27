package com.example.ridelink_ride_service.security;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
public record Identity(Long accountId, String name, String role) {
    public void require(String expected) {
        if (!expected.equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This operation requires role " + expected);
    }
}
