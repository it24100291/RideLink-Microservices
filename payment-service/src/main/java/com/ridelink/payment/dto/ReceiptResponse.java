package com.ridelink.payment.dto;

import com.ridelink.payment.entity.Receipt;

import java.math.BigDecimal;
import java.time.Instant;

public record ReceiptResponse(
        Long paymentId,
        String receiptNumber,
        Long rideId,
        BigDecimal amount,
        String paymentReference,
        Instant issuedAt
) {
    public static ReceiptResponse from(Receipt receipt) {
        return new ReceiptResponse(receipt.getPaymentId(), receipt.getReceiptNumber(),
                receipt.getRideId(), receipt.getAmount(), receipt.getPaymentReference(), receipt.getIssuedAt());
    }
}
