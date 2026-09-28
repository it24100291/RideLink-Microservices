package com.ridelink.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record FareCalculationRequest(
        @NotNull @Positive Long rideId,
        @NotNull @Positive BigDecimal distanceKm,
        @NotNull @PositiveOrZero Integer durationMinutes
) {
}
