package com.ridelink.payment.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class HttpRideClient implements RideGateway {
    private final RestClient client;

    public HttpRideClient(@Value("${ride.service.base-url:http://localhost:8082}") String url) {
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    @Override
    public RideDetails getRide(Long rideId, String authorization) {
        try {
            return client.get().uri("/api/rides/{id}", rideId)
                    .header("Authorization", authorization)
                    .retrieve().body(RideDetails.class);
        } catch (RestClientResponseException exception) {
            HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
            if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN
                    || status == HttpStatus.NOT_FOUND) {
                throw new ResponseStatusException(status, switch (status) {
                    case UNAUTHORIZED -> "Ride Service rejected authentication.";
                    case FORBIDDEN -> "Ride access is forbidden.";
                    default -> "Ride not found.";
                });
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Ride Service is unavailable.");
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Ride Service is unavailable.");
        }
    }
}
