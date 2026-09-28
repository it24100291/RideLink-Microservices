package com.ridelink.payment.dto;

import com.ridelink.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record PaymentRequest(
        @Schema(description = "Unique ride identifier", example = "42")
        @NotNull @Positive Long rideId,
        @Schema(description = "Completed ride distance in kilometers", example = "10.5")
        @NotNull @Positive BigDecimal distanceKm,
        @Schema(description = "Completed ride duration in minutes", example = "24")
        @NotNull @PositiveOrZero Integer durationMinutes,
        @Schema(description = "Payment method used for the simulation", example = "CARD")
        @NotNull PaymentMethod paymentMethod
) {
}
