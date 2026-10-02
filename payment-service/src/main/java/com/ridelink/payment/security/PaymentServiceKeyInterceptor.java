package com.ridelink.payment.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PaymentServiceKeyInterceptor implements HandlerInterceptor {
    private final String serviceKey;

    public PaymentServiceKeyInterceptor(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (serviceKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Payment service credential is not configured");
        }
        String supplied = request.getHeader("X-Service-Key");
        if (supplied == null || !MessageDigest.isEqual(serviceKey.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid service credential");
        }
        return true;
    }
}
