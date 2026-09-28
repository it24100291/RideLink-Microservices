package com.ridelink.payment.dto;

import java.math.BigDecimal;

public record FareCalculationResponse(
        Long rideId,
        BigDecimal baseFare,
        BigDecimal distanceCharge,
        BigDecimal durationCharge,
        BigDecimal totalFare
) {
}
