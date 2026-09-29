package com.example.ridelink_ride_service.model;

public enum RideStatus {
    REQUESTED,
    ASSIGNED,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public static RideStatus fromStoredValue(String value) {
        if (value == null) {
            throw new IllegalStateException("Ride status is missing");
        }
        return switch (value) {
            // Values written by older versions remain readable; all new writes use the active lifecycle.
            case "PENDING" -> REQUESTED;
            case "CONFIRMED" -> ASSIGNED;
            default -> RideStatus.valueOf(value);
        };
    }
}
