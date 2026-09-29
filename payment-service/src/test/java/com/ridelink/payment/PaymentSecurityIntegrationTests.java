package com.ridelink.payment;

import com.ridelink.payment.repository.PaymentRepository;
import com.ridelink.payment.repository.ReceiptRepository;
import com.ridelink.payment.entity.Payment;
import com.ridelink.payment.entity.PaymentMethod;
import com.ridelink.payment.entity.PaymentStatus;
import org.springframework.dao.DataIntegrityViolationException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentSecurityIntegrationTests {
    private static final String SERVICE_KEY = "payment-test-service-key";
    private static HttpServer accountServer;
    private static HttpServer rideServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository payments;

    @Autowired
    private ReceiptRepository receipts;

    @DynamicPropertySource
    static void accountServiceProperties(DynamicPropertyRegistry registry) {
        startAccountStub();
        startRideStub();
        registry.add("account.service.base-url", () -> "http://127.0.0.1:" + accountServer.getAddress().getPort());
        registry.add("ride.service.base-url", () -> "http://127.0.0.1:" + rideServer.getAddress().getPort());
        registry.add("integration.account-service-key", () -> SERVICE_KEY);
    }

    private static synchronized void startRideStub() {
        if (rideServer != null) return;
        try {
            rideServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            rideServer.createContext("/api/rides/", exchange -> {
                String authorization = exchange.getRequestHeaders().getFirst("Authorization");
                int responseStatus = 200;
                String responseBody;
                if (authorization == null || !authorization.startsWith("Bearer ")) {
                    responseStatus = 401;
                    responseBody = "{}";
                } else {
                    String pathId = exchange.getRequestURI().getPath().substring("/api/rides/".length());
                    responseBody = switch (pathId) {
                        case "42" -> "{\"id\":42,\"passengerAccountId\":101,\"status\":\"COMPLETED\"}";
                        case "43" -> "{\"id\":43,\"passengerAccountId\":101,\"status\":\"CONFIRMED\"}";
                        case "44" -> "{\"id\":44,\"passengerAccountId\":101,\"status\":\"IN_PROGRESS\"}";
                        case "45" -> "{\"id\":45,\"passengerAccountId\":101,\"status\":\"CANCELLED\"}";
                        case "46" -> "{\"id\":46,\"passengerAccountId\":202,\"status\":\"COMPLETED\"}";
                        case "48" -> "{\"id\":480,\"passengerAccountId\":101,\"status\":\"COMPLETED\"}";
                        case "403" -> { responseStatus = 403; yield "{}"; }
                        case "404" -> { responseStatus = 404; yield "{}"; }
                        default -> { responseStatus = 503; yield "{}"; }
                    };
                }
                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(responseStatus, bytes.length);
                try (var body = exchange.getResponseBody()) { body.write(bytes); }
            });
            rideServer.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start Ride Service test stub", exception);
        }
    }

    private static synchronized void startAccountStub() {
        if (accountServer != null) {
            return;
        }
        if (rideServer != null) {
            rideServer.stop(0);
            rideServer = null;
        }
        try {
            accountServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            accountServer.createContext("/internal/auth/introspect", exchange -> {
                int responseStatus = 200;
                String responseBody;
                if (!SERVICE_KEY.equals(exchange.getRequestHeaders().getFirst("X-Service-Key"))) {
                    responseStatus = 403;
                    responseBody = "{}";
                } else {
                    String authorization = exchange.getRequestHeaders().getFirst("Authorization");
                    responseBody = switch (authorization == null ? "" : authorization) {
                        case "Bearer passenger-one" -> "{\"accountId\":101,\"name\":\"Passenger One\",\"role\":\"PASSENGER\"}";
                        case "Bearer passenger-two" -> "{\"accountId\":202,\"name\":\"Passenger Two\",\"role\":\"PASSENGER\"}";
                        case "Bearer administrator" -> "{\"accountId\":999,\"name\":\"Admin\",\"role\":\"ADMIN\"}";
                        case "Bearer driver" -> "{\"accountId\":303,\"name\":\"Driver\",\"role\":\"DRIVER\"}";
                        default -> {
                            responseStatus = 401;
                            yield "{}";
                        }
                    };
                }
                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(responseStatus, bytes.length);
                try (var body = exchange.getResponseBody()) {
                    body.write(bytes);
                }
            });
            accountServer.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start Account Service test stub", exception);
        }
    }

    @AfterAll
    static void stopAccountStub() {
        if (accountServer != null) {
            accountServer.stop(0);
            accountServer = null;
        }
    }

    @BeforeEach
    void clearPaymentData() {
        receipts.deleteAll();
        payments.deleteAll();
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void rejectedAccountIntrospectionIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/payments").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void passengerCreatesPaymentOwnedByIntrospectedAccount() throws Exception {
        String response = mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer passenger-one")
                        .contentType("application/json")
                        .content(paymentRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        Number paymentIdValue = JsonPath.read(response, "$.id");
        Long paymentId = paymentIdValue.longValue();
        var payment = payments.findById(paymentId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(101L, payment.getPassengerAccountId());
    }

    @Test
    void cannotCreatePaymentForRideOwnedByAnotherPassenger() throws Exception {
        mockMvc.perform(post("/api/payments").header("Authorization", "Bearer passenger-one")
                        .contentType("application/json").content(paymentRequest(46)))
                .andExpect(status().isForbidden());
        assertEquals(0, payments.count());
    }

    @Test
    void rideMustExistAndBeCompletedBeforePayment() throws Exception {
        mockMvc.perform(post("/api/payments").header("Authorization", "Bearer passenger-one")
                        .contentType("application/json").content(paymentRequest(404)))
                .andExpect(status().isNotFound());
        for (long rideId : new long[]{43, 44, 45}) {
            mockMvc.perform(post("/api/payments").header("Authorization", "Bearer passenger-one")
                            .contentType("application/json").content(paymentRequest(rideId)))
                    .andExpect(status().isConflict());
        }
        assertEquals(0, payments.count());
    }

    @Test
    void rideServiceFailuresAndMismatchedRideAreUnavailable() throws Exception {
        for (long rideId : new long[]{48, 500}) {
            mockMvc.perform(post("/api/payments").header("Authorization", "Bearer passenger-one")
                            .contentType("application/json").content(paymentRequest(rideId)))
                    .andExpect(status().isServiceUnavailable());
        }
        mockMvc.perform(post("/api/payments").header("Authorization", "Bearer passenger-one")
                        .contentType("application/json").content(paymentRequest(403)))
                .andExpect(status().isForbidden());
        assertEquals(0, payments.count());
    }

    @Test
    void duplicatePaymentForRideIsRejectedAndDatabaseConstraintIsUnique() throws Exception {
        createPayment("passenger-one");
        mockMvc.perform(post("/api/payments").header("Authorization", "Bearer passenger-one")
                        .contentType("application/json").content(paymentRequest()))
                .andExpect(status().isConflict());
        assertEquals(1, payments.countByRideId(42L));
        assertThrows(DataIntegrityViolationException.class, () -> payments.saveAndFlush(
                new Payment(42L, 101L, java.math.BigDecimal.TEN, PaymentMethod.CARD,
                        PaymentStatus.PENDING, "direct-duplicate")));
        assertEquals(1, payments.countByRideId(42L));
    }

    @Test
    void successfulOwnerCanRetrieveReceipt() throws Exception {
        long paymentId = createPayment("passenger-one");
        mockMvc.perform(put("/api/payments/{id}/status", paymentId)
                        .header("Authorization", "Bearer administrator")
                        .contentType("application/json").content("{\"status\":\"SUCCESS\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/payments/{id}/receipt", paymentId)
                        .header("Authorization", "Bearer passenger-one"))
                .andExpect(status().isOk());
    }

    @Test
    void passengerCanRetrieveOwnPayment() throws Exception {
        long paymentId = createPayment("passenger-one");
        mockMvc.perform(get("/api/payments/{id}", paymentId)
                        .header("Authorization", "Bearer passenger-one"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value(42));
    }

    @Test
    void anotherPassengerCannotRetrievePaymentOrReceipt() throws Exception {
        long paymentId = createPayment("passenger-one");
        mockMvc.perform(get("/api/payments/{id}", paymentId)
                        .header("Authorization", "Bearer passenger-two"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(put("/api/payments/{id}/status", paymentId)
                        .header("Authorization", "Bearer administrator")
                        .contentType("application/json").content("{\"status\":\"SUCCESS\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/payments/{id}/receipt", paymentId)
                        .header("Authorization", "Bearer passenger-two"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void passengerCannotAccessAdminPaymentListOrSummary() throws Exception {
        mockMvc.perform(get("/api/payments").header("Authorization", "Bearer passenger-one"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/payments/summary").header("Authorization", "Bearer passenger-one"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/payments").header("Authorization", "Bearer administrator"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/payments/summary").header("Authorization", "Bearer administrator"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPayments").value(0));
    }

    @Test
    void fareEndpointRejectsUnauthenticatedCaller() throws Exception {
        mockMvc.perform(post("/api/fares/calculate")
                        .contentType("application/json")
                        .content("{\"rideId\":42,\"distanceKm\":10,\"durationMinutes\":20}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passengerCannotSetPaymentStatusAndDriverCannotCreatePayment() throws Exception {
        long paymentId = createPayment("passenger-one");
        mockMvc.perform(put("/api/payments/{id}/status", paymentId)
                        .header("Authorization", "Bearer passenger-one")
                        .contentType("application/json").content("{\"status\":\"SUCCESS\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer driver")
                        .contentType("application/json")
                        .content(paymentRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedMissingPaymentReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/payments/99999").header("Authorization", "Bearer passenger-one"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private long createPayment(String token) throws Exception {
        String response = mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(paymentRequest()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number paymentIdValue = JsonPath.read(response, "$.id");
        return paymentIdValue.longValue();
    }

    private String paymentRequest() {
        return paymentRequest(42);
    }

    private String paymentRequest(long rideId) {
        return "{\"rideId\":" + rideId + ",\"distanceKm\":10.0,\"durationMinutes\":20,\"paymentMethod\":\"CARD\"}";
    }
}
