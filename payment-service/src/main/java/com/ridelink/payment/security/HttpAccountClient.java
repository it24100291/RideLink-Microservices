package com.ridelink.payment.security;

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
public class HttpAccountClient implements AccountClient {
    private final RestClient client;
    private final String key;

    public HttpAccountClient(
            @Value("${account.service.base-url:http://localhost:8083}") String url,
            @Value("${integration.account-service-key:}") String key) {
        this.key = key;
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    @Override
    public Identity authenticate(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                || authorization.substring(7).isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token required.");
        }
        if (key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Account service credential is not configured.");
        }
        try {
            Identity identity = client.post().uri("/internal/auth/introspect")
                    .header("Authorization", authorization)
                    .header("X-Service-Key", key)
                    .retrieve().body(Identity.class);
            if (identity == null || identity.accountId() == null || identity.role() == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Invalid authentication response.");
            }
            return identity;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid, expired, revoked or suspended account token.");
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Account authentication is unavailable.");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Account authentication is unavailable.");
        }
    }
}
