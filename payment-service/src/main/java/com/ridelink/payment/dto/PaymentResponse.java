package com.ridelink.payment.dto;

import com.ridelink.payment.entity.Payment;
import com.ridelink.payment.entity.PaymentMethod;
import com.ridelink.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long rideId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String referenceId,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getRideId(),
                payment.getAmount(), payment.getPaymentMethod(), payment.getStatus(),
                payment.getReferenceId(), payment.getCreatedAt(), payment.getUpdatedAt());
    }
}
