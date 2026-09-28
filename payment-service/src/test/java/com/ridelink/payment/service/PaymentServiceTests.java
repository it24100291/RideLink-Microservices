package com.ridelink.payment.service;

import com.ridelink.payment.dto.FareCalculationRequest;
import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.dto.PaymentResponse;
import com.ridelink.payment.entity.Payment;
import com.ridelink.payment.entity.PaymentMethod;
import com.ridelink.payment.entity.PaymentStatus;
import com.ridelink.payment.entity.Receipt;
import com.ridelink.payment.exception.InvalidPaymentException;
import com.ridelink.payment.exception.PaymentNotFoundException;
import com.ridelink.payment.repository.PaymentRepository;
import com.ridelink.payment.repository.ReceiptRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTests {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    private PaymentService paymentService;

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        paymentService = new PaymentService(paymentRepository, receiptRepository, new BigDecimal("2.00"),
                new BigDecimal("1.50"), new BigDecimal("0.25"));
    }

    @Test
    void calculatesFareUsingConfiguredRates() {
        var result = paymentService.calculateFare(new FareCalculationRequest(
                1L, new BigDecimal("10"), 20));

        assertEquals(new BigDecimal("22.00"), result.totalFare());
        assertEquals(new BigDecimal("15.00"), result.distanceCharge());
        assertEquals(new BigDecimal("5.00"), result.durationCharge());
    }

    @Test
    void fareRequestRejectsInvalidValues() {
        var violations = validator.validate(new FareCalculationRequest(
                null, new BigDecimal("-1"), -2));

        assertEquals(3, violations.size());
    }

    @Test
    void paymentRequestRequiresValidValues() {
        var violations = validator.validate(new PaymentRequest(
                null, BigDecimal.ZERO, -1, null));

        assertEquals(4, violations.size());
    }

    @Test
    void createsPaymentWithPendingStatus() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPayment(new PaymentRequest(
                1L, new BigDecimal("10"), 20, PaymentMethod.CARD));

        assertEquals(PaymentStatus.PENDING, response.status());
        assertEquals(new BigDecimal("22.00"), response.amount());
        assertTrue(response.referenceId() != null && !response.referenceId().isBlank());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void retrievesPaymentById() {
        Payment payment = payment(PaymentStatus.PENDING);
        when(paymentRepository.findById(7L)).thenReturn(Optional.of(payment));

        var response = paymentService.getPayment(7L);

        assertEquals(7L, response.id());
        assertEquals(1L, response.rideId());
    }

    @Test
    void retrievesAllPayments() {
        when(paymentRepository.findAll()).thenReturn(List.of(payment(PaymentStatus.SUCCESS)));

        var responses = paymentService.getAllPayments();

        assertEquals(List.of(PaymentResponse.from(payment(PaymentStatus.SUCCESS))), responses);
    }

    @Test
    void calculatesPaymentSummaryUsingOnlySuccessfulAmountsAsRevenue() {
        when(paymentRepository.count()).thenReturn(5L);
        when(paymentRepository.countByStatus(PaymentStatus.SUCCESS)).thenReturn(4L);
        when(paymentRepository.countByStatus(PaymentStatus.PENDING)).thenReturn(1L);
        when(paymentRepository.countByStatus(PaymentStatus.REFUNDED)).thenReturn(0L);
        when(paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS)).thenReturn(new BigDecimal("85.50"));

        var summary = paymentService.getPaymentSummary();

        assertEquals(5L, summary.totalPayments());
        assertEquals(4L, summary.successfulPayments());
        assertEquals(1L, summary.pendingPayments());
        assertEquals(0L, summary.refundedPayments());
        assertEquals(new BigDecimal("85.50"), summary.totalRevenue());
        verify(paymentRepository).sumAmountByStatus(PaymentStatus.SUCCESS);
    }

    @Test
    void missingPaymentRaisesNotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPayment(99L));
    }

    @Test
    void paymentStatusCanSucceedAndThenBeRefundedButCannotRepeat() {
        Payment payment = payment(PaymentStatus.PENDING);
        when(paymentRepository.findById(7L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(PaymentStatus.SUCCESS, paymentService.updateStatus(7L, PaymentStatus.SUCCESS).status());
        verify(receiptRepository).save(any(Receipt.class));
        assertEquals(PaymentStatus.REFUNDED, paymentService.updateStatus(7L, PaymentStatus.REFUNDED).status());
        assertThrows(InvalidPaymentException.class,
                () -> paymentService.updateStatus(7L, PaymentStatus.SUCCESS));
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment(1L, new BigDecimal("12.00"),
                PaymentMethod.CARD, status, "reference-1");
        ReflectionTestUtils.setField(payment, "id", 7L);
        return payment;
    }
}
