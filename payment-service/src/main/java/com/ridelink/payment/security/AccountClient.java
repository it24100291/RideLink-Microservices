package com.ridelink.payment.security;

public interface AccountClient {
    Identity authenticate(String authorization);
}
