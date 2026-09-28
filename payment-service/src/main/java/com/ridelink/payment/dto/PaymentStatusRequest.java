package com.ridelink.payment.dto;

import com.ridelink.payment.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record PaymentStatusRequest(@NotNull PaymentStatus status) {
}
