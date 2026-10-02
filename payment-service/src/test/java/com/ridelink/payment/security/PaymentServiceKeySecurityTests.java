package com.ridelink.payment.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.payment.repository.PaymentRepository;
import com.ridelink.payment.repository.ReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "payment.internal.service-key=test-payment-key")
@AutoConfigureMockMvc
class PaymentServiceKeySecurityTests {
    private static final String KEY = "test-payment-key";
    private static final String PAYMENT = "{\"rideId\":42,\"distanceKm\":10,\"durationMinutes\":20,\"paymentMethod\":\"CARD\"}";
    private static final String STATUS = "{\"status\":\"SUCCESS\"}";
    private static final String FARE = "{\"rideId\":43,\"distanceKm\":10,\"durationMinutes\":20}";

    @Autowired MockMvc mvc;
    @Autowired PaymentRepository payments;
    @Autowired ReceiptRepository receipts;
    @Autowired ObjectMapper mapper;

    @BeforeEach
    void clearDatabase() {
        receipts.deleteAll();
        payments.deleteAll();
    }

    @Test
    void everyPaymentAndFareEndpointRejectsMissingOrInvalidServiceKey() throws Exception {
        assertRejected(post("/api/payments").content(PAYMENT));
        assertRejected(get("/api/payments/1"));
        assertRejected(get("/api/payments/1/receipt"));
        assertRejected(get("/api/payments"));
        assertRejected(get("/api/payments/summary"));
        assertRejected(get("/api/payments/ride/42"));
        assertRejected(put("/api/payments/1/status").content(STATUS));
        assertRejected(post("/api/payments/1/refund"));
        assertRejected(post("/api/fares/calculate").content(FARE));
    }

    @Test
    void validServiceKeyCanUsePaymentLifecycleAndFareEndpoints() throws Exception {
        MvcResult created = mvc.perform(post("/api/payments").header("X-Service-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(PAYMENT))
                .andExpect(status().isCreated()).andReturn();
        JsonNode payment = mapper.readTree(created.getResponse().getContentAsString());
        long id = payment.get("id").asLong();

        mvc.perform(get("/api/payments/{id}", id).header("X-Service-Key", KEY)).andExpect(status().isOk());
        mvc.perform(get("/api/payments").header("X-Service-Key", KEY)).andExpect(status().isOk());
        mvc.perform(get("/api/payments/summary").header("X-Service-Key", KEY)).andExpect(status().isOk());
        mvc.perform(get("/api/payments/ride/42").header("X-Service-Key", KEY)).andExpect(status().isOk());
        mvc.perform(put("/api/payments/{id}/status", id).header("X-Service-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(STATUS))
                .andExpect(status().isOk());
        mvc.perform(get("/api/payments/{id}/receipt", id).header("X-Service-Key", KEY))
                .andExpect(status().isOk());
        mvc.perform(post("/api/payments/{id}/refund", id).header("X-Service-Key", KEY))
                .andExpect(status().isOk());
        mvc.perform(post("/api/fares/calculate").header("X-Service-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(FARE))
                .andExpect(status().isOk());

        assertThat(payment.get("status").asText()).isEqualTo("PENDING");
    }

    private void assertRejected(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        request.contentType(MediaType.APPLICATION_JSON);
        mvc.perform(request).andExpect(status().isUnauthorized());
        request.header("X-Service-Key", "wrong-key");
        mvc.perform(request).andExpect(status().isUnauthorized());
    }
}
