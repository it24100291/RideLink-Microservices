package com.ridelink.payment.client;

public interface RideGateway {
    RideDetails getRide(Long rideId, String authorization);
}
