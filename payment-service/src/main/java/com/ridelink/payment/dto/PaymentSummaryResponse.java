package com.ridelink.payment.dto;

import java.math.BigDecimal;

public record PaymentSummaryResponse(
        long totalPayments,
        long successfulPayments,
        long pendingPayments,
        long refundedPayments,
        BigDecimal totalRevenue
) {
}
