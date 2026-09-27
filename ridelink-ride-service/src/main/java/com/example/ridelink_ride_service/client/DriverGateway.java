package com.example.ridelink_ride_service.client;
public interface DriverGateway {
    Reservation reserve(String id);
    void release(String id);
    record Reservation(String id, Long driverId, Long driverAccountId, boolean released) { }
}
