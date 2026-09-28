package com.ridelink.payment.controller;

import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.dto.PaymentResponse;
import com.ridelink.payment.dto.PaymentStatusRequest;
import com.ridelink.payment.dto.PaymentSummaryResponse;
import com.ridelink.payment.dto.ReceiptResponse;
import com.ridelink.payment.service.PaymentService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Record simulated payments, update payment status, and retrieve receipts")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a simulated payment",
            description = "Calculates the fare on the server from ride distance and duration, then records the payment as PENDING.")
    public PaymentResponse create(@Valid @RequestBody PaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public PaymentResponse getById(@PathVariable Long id) {
        return paymentService.getPayment(id);
    }

    @GetMapping("/{id}/receipt")
    @Operation(summary = "Retrieve a payment receipt",
            description = "Returns the receipt generated when the payment transitions to SUCCESS.")
    public ReceiptResponse getReceipt(@PathVariable Long id) {
        return paymentService.getReceipt(id);
    }

    @GetMapping
    @Operation(summary = "List payments")
    public List<PaymentResponse> getAll() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/summary")
    @Operation(summary = "Get payment summary")
    public PaymentSummaryResponse getSummary() {
        return paymentService.getPaymentSummary();
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "List payments for a ride")
    public List<PaymentResponse> getByRide(@PathVariable Long rideId) {
        return paymentService.getPaymentsForRide(rideId);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update payment status",
            description = "Simulates a payment result. PENDING payments can become SUCCESS or FAILED; SUCCESS payments can become REFUNDED.")
    public PaymentResponse updateStatus(@PathVariable Long id, @Valid @RequestBody PaymentStatusRequest request) {
        return paymentService.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund a successful payment")
    public PaymentResponse refund(@PathVariable Long id) {
        return paymentService.updateStatus(id,
                com.ridelink.payment.entity.PaymentStatus.REFUNDED);
    }
}
