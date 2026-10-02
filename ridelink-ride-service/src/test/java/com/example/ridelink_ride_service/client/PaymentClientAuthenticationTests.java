package com.example.ridelink_ride_service.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentClientAuthenticationTests {
    private HttpServer server;
    private final List<String> receivedKeys = new CopyOnWriteArrayList<>();
    private PaymentClient client;

    @BeforeEach
    void startPaymentStub() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/payments", exchange -> {
            receivedKeys.add(exchange.getRequestHeaders().getFirst("X-Service-Key"));
            String path = exchange.getRequestURI().getPath();
            String body;
            if (path.endsWith("/receipt")) {
                body = "{\"paymentId\":7,\"receiptNumber\":\"receipt-7\",\"rideId\":42,\"amount\":22.00,\"paymentReference\":\"ref-7\",\"issuedAt\":\"2026-01-01T00:00:00Z\"}";
            } else if ("GET".equals(exchange.getRequestMethod()) && path.endsWith("/42")) {
                body = "[{\"id\":7,\"rideId\":42,\"amount\":22.00,\"paymentMethod\":\"CARD\",\"status\":\"PENDING\",\"referenceId\":\"ref-7\"}]";
            } else {
                body = "{\"id\":7,\"rideId\":42,\"amount\":22.00,\"paymentMethod\":\"CARD\",\"status\":\"PENDING\",\"referenceId\":\"ref-7\"}";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        client = new PaymentClient("http://127.0.0.1:" + server.getAddress().getPort(), "test-payment-key");
    }

    @AfterEach
    void stopPaymentStub() {
        server.stop(0);
    }

    @Test
    void sendsConfiguredServiceKeyForEveryRideToPaymentCall() {
        client.create(42L, new BigDecimal("10"), 20, PaymentGateway.PaymentMethod.CARD);
        client.get(42L);
        client.updateStatus(42L, PaymentGateway.PaymentStatus.SUCCESS);
        client.getReceipt(42L);

        assertThat(receivedKeys).hasSize(6).containsOnly("test-payment-key");
    }
}
