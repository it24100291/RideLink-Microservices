package com.example.ridelink_ride_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Component
public class PaymentClient implements PaymentGateway {
    private final RestClient client;

    public PaymentClient(@Value("${payment.service.base-url:http://localhost:8084}") String url) {
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    @Override
    public Payment create(Long rideId, BigDecimal distanceKm, Integer durationMinutes, PaymentMethod method) {
        try {
            Payment payment = client.post().uri("/api/payments")
                    .body(new CreatePaymentRequest(rideId, distanceKm, durationMinutes, method))
                    .retrieve().body(Payment.class);
            if (payment == null || !rideId.equals(payment.rideId())) throw unavailable();
            return payment;
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (RestClientException ex) {
            throw unavailable();
        }
    }

    @Override
    public Payment get(Long rideId) {
        try {
            List<Payment> payments = client.get().uri("/api/payments/ride/{rideId}", rideId)
                    .retrieve().body(new ParameterizedTypeReference<>() { });
            if (payments == null || payments.isEmpty())
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No payment found for ride " + rideId);
            if (payments.size() != 1 || !rideId.equals(payments.get(0).rideId())) throw unavailable();
            return payments.get(0);
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (RestClientException ex) {
            throw unavailable();
        }
    }

    @Override
    public Payment updateStatus(Long rideId, PaymentStatus status) {
        Payment payment = get(rideId);
        try {
            Payment updated = client.put().uri("/api/payments/{id}/status", payment.id())
                    .body(new StatusRequest(status)).retrieve().body(Payment.class);
            if (updated == null || !rideId.equals(updated.rideId())) throw unavailable();
            return updated;
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (RestClientException ex) {
            throw unavailable();
        }
    }

    @Override
    public Receipt getReceipt(Long rideId) {
        Payment payment = get(rideId);
        try {
            Receipt receipt = client.get().uri("/api/payments/{id}/receipt", payment.id())
                    .retrieve().body(Receipt.class);
            if (receipt == null || !rideId.equals(receipt.rideId())) throw unavailable();
            return receipt;
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (RestClientException ex) {
            throw unavailable();
        }
    }

    private ResponseStatusException translate(RestClientResponseException ex) {
        return switch (ex.getStatusCode().value()) {
            case 400 -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment request was rejected");
            case 404 -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment or receipt was not found");
            case 409 -> new ResponseStatusException(HttpStatus.CONFLICT, "Payment request conflicts with the existing payment state");
            default -> unavailable();
        };
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Payment service is unavailable or returned an invalid response");
    }

    private record CreatePaymentRequest(Long rideId, BigDecimal distanceKm, Integer durationMinutes,
                                        PaymentMethod paymentMethod) { }
    private record StatusRequest(PaymentStatus status) { }
}
