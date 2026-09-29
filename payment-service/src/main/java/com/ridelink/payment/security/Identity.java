package com.ridelink.payment.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record Identity(Long accountId, String name, String role) {
    public void require(String expected) {
        if (!expected.equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This operation requires role " + expected);
        }
    }

    public void requireOwnerOrAdmin(Long ownerAccountId) {
        if ("ADMIN".equals(role)) {
            return;
        }
        require("PASSENGER");
        if (!accountId.equals(ownerAccountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This payment belongs to another account");
        }
    }

    public void requirePassengerOrAdmin() {
        if (!"ADMIN".equals(role) && !"PASSENGER".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This operation requires a passenger or administrator account");
        }
    }
}
