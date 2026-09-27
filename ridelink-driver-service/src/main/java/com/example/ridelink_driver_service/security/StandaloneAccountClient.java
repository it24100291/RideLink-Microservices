package com.example.ridelink_driver_service.security;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.LoggerFactory;

@Component
@Profile("standalone")
public class StandaloneAccountClient implements AccountClient {
    public StandaloneAccountClient() {
        LoggerFactory.getLogger(getClass()).warn("STANDALONE DEVELOPMENT MODE: fixed test identities are enabled. Do not expose this service publicly.");
    }
    public Identity authenticate(String authorization) {
        if ("Bearer dev-passenger-1".equals(authorization)) return new Identity(1L, "Demo Passenger", "PASSENGER");
        if ("Bearer dev-driver-2".equals(authorization)) return new Identity(2L, "Demo Driver", "DRIVER");
        if ("Bearer dev-admin-3".equals(authorization)) return new Identity(3L, "Demo Admin", "ADMIN");
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Use a documented standalone development token.");
    }
}
