package com.example.ridelink_ride_service.client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.HttpClient;
import java.time.Duration;

@Component
@Profile("!standalone")
public class DriverClient implements DriverGateway {
    private final RestClient client;
    private final String key;
    public DriverClient(@Value("${driver.service.base-url:http://localhost:8081}") String url,
            @Value("${integration.ride-service-key:}") String key) {
        this.key=key;
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        client=RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
    private void configured() {
        if (key.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Driver service credential is not configured");
    }
    public Reservation reserve(String id) {
        configured();
        try {
            var result=client.put().uri("/internal/reservations/{id}",id).header("X-Service-Key",key).retrieve().body(Reservation.class);
            if(result==null || result.driverId()==null || result.driverAccountId()==null || result.released())
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Invalid Driver reservation response");
            return result;
        } catch(RestClientResponseException ex) {
            if(ex.getStatusCode().value()==409) throw new ResponseStatusException(HttpStatus.CONFLICT,"No driver can be reserved. Retry with a new booking key.");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Driver service is unavailable");
        } catch(RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Driver service is unavailable");
        }
    }
    public void release(String id) {
        configured();
        try { client.delete().uri("/internal/reservations/{id}",id).header("X-Service-Key",key).retrieve().toBodilessEntity(); }
        catch(RestClientException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Driver release is pending; it will be retried"); }
    }
}
