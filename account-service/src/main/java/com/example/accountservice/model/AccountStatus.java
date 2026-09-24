package com.example.accountservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AccountStatus {
    ACTIVE,
    SUSPENDED;

    @JsonCreator
    public static AccountStatus fromString(String value) {
        if (value == null) {
            return null;
        }

        for (AccountStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown account status: " + value);
    }
}
