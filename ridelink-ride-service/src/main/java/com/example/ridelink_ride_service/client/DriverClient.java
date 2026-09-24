package com.example.ridelink_ride_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class DriverClient {

    private final RestClient restClient;

    public DriverClient(@Value("${driver.service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public List<DriverResponse> getAvailableDrivers() {

        DriverResponse[] drivers = restClient.get()
                .uri("/api/drivers/available")
                .retrieve()
                .body(DriverResponse[].class);

        return drivers == null ? List.of() : List.of(drivers);
    }

    public DriverResponse updateAvailability(Long driverId, boolean available) {

        return restClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/drivers/{id}/availability")
                        .queryParam("available", available)
                        .build(driverId))
                .retrieve()
                .body(DriverResponse.class);
    }

    public record DriverResponse(
            Long id,
            String name,
            String licenseNumber,
            boolean available
    ) {
    }
}