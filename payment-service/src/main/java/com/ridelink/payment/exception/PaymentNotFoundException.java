package com.ridelink.payment.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(Long id) {
        super("Payment %d was not found".formatted(id));
    }
}
