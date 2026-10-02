package com.example.ridelink_ride_service.client;

import java.math.BigDecimal;

public interface PaymentGateway {
    Payment create(Long rideId, BigDecimal distanceKm, Integer durationMinutes, PaymentMethod method);
    Payment get(Long rideId);
    Payment updateStatus(Long rideId, PaymentStatus status);
    Receipt getReceipt(Long rideId);

    record Payment(Long id, Long rideId, BigDecimal amount, PaymentMethod paymentMethod,
                   PaymentStatus status, String referenceId) { }
    record Receipt(Long paymentId, String receiptNumber, Long rideId, BigDecimal amount,
                   String paymentReference, String issuedAt) { }
    enum PaymentMethod { CARD, CASH, WALLET, ONLINE }
    enum PaymentStatus { PENDING, SUCCESS, FAILED, REFUNDED }
}
