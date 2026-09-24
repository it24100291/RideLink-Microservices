package com.example.accountservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Role {
    PASSENGER,
    DRIVER,
    ADMIN;

    @JsonCreator
    public static Role fromString(String value) {
        if (value == null) {
            return null;
        }

        for (Role role : values()) {
            if (role.name().equalsIgnoreCase(value.trim())) {
                return role;
            }
        }

        throw new IllegalArgumentException("Unknown role: " + value);
    }
}
