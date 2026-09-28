package com.ridelink.payment.controller;

import com.ridelink.payment.dto.FareCalculationRequest;
import com.ridelink.payment.dto.FareCalculationResponse;
import com.ridelink.payment.service.PaymentService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fares")
@Tag(name = "Fares", description = "Estimate ride fares using the configured fare formula")
public class FareController {
    private final PaymentService paymentService;

    public FareController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/calculate")
    @Operation(summary = "Calculate a ride fare",
            description = "Calculates base fare + distance charge + duration charge. Each component is rounded to two decimal places.")
    public FareCalculationResponse calculate(@Valid @RequestBody FareCalculationRequest request) {
        return paymentService.calculateFare(request);
    }
}
