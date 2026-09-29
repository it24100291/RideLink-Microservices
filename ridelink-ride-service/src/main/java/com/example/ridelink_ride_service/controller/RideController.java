package com.example.ridelink_ride_service.controller;
import com.example.ridelink_ride_service.model.Ride;
import com.example.ridelink_ride_service.service.RideService;
import com.example.ridelink_ride_service.security.Identity;
import com.example.ridelink_ride_service.client.PaymentGateway;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/rides")
@Validated
public class RideController {
    private final RideService rides;
    public RideController(RideService rides){this.rides=rides;}
    @PostMapping
    public Ride create(@RequestAttribute("identity") Identity identity,
            @RequestHeader(value="Idempotency-Key",required=false) UUID key,@Valid @RequestBody Booking request) {
        return rides.create(identity,key==null?UUID.randomUUID():key,request.pickup(),request.destination());
    }
    @GetMapping public List<Ride> mine(@RequestAttribute("identity") Identity identity){return rides.mine(identity);}
    @GetMapping("/{id}") public Ride get(@RequestAttribute("identity") Identity identity,@PathVariable Long id){return rides.get(id,identity);}
    @PatchMapping("/{id}/{action}")
    public Ride transition(@RequestAttribute("identity") Identity identity,@PathVariable Long id,@PathVariable String action) {
        return rides.transition(id,identity,action);
    }
    @PostMapping("/{id}/payment")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentGateway.Payment createPayment(@RequestAttribute("identity") Identity identity,
            @PathVariable @Positive Long id, @Valid @RequestBody RidePayment request) {
        return rides.createPayment(id, identity, request.distanceKm(), request.durationMinutes(), request.paymentMethod());
    }
    @GetMapping("/{id}/payment")
    public PaymentGateway.Payment getPayment(@RequestAttribute("identity") Identity identity,
            @PathVariable @Positive Long id) {
        return rides.getPayment(id, identity);
    }
    @PutMapping("/{id}/payment/status")
    public PaymentGateway.Payment updatePaymentStatus(@RequestAttribute("identity") Identity identity,
            @PathVariable @Positive Long id, @Valid @RequestBody RidePaymentStatus request) {
        return rides.updatePaymentStatus(id, identity, request.status());
    }
    @GetMapping("/{id}/payment/receipt")
    public PaymentGateway.Receipt getReceipt(@RequestAttribute("identity") Identity identity,
            @PathVariable @Positive Long id) {
        return rides.getReceipt(id, identity);
    }
    public record Booking(@NotBlank @Size(max=255) String pickup,@NotBlank @Size(max=255) String destination){ }
    public record RidePayment(@NotNull @Positive java.math.BigDecimal distanceKm,
                              @NotNull @PositiveOrZero Integer durationMinutes,
                              @NotNull PaymentGateway.PaymentMethod paymentMethod) { }
    public record RidePaymentStatus(@NotNull PaymentGateway.PaymentStatus status) { }
}
