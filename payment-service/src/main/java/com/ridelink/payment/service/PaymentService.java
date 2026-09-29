package com.ridelink.payment.service;

import com.ridelink.payment.dto.FareCalculationRequest;
import com.ridelink.payment.dto.FareCalculationResponse;
import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.dto.PaymentResponse;
import com.ridelink.payment.dto.PaymentSummaryResponse;
import com.ridelink.payment.dto.ReceiptResponse;
import com.ridelink.payment.entity.Payment;
import com.ridelink.payment.entity.Receipt;
import com.ridelink.payment.entity.PaymentStatus;
import com.ridelink.payment.exception.InvalidPaymentException;
import com.ridelink.payment.exception.PaymentNotFoundException;
import com.ridelink.payment.repository.PaymentRepository;
import com.ridelink.payment.repository.ReceiptRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;
    private final BigDecimal baseFare;
    private final BigDecimal perKmRate;
    private final BigDecimal perMinuteRate;

    public PaymentService(PaymentRepository paymentRepository, ReceiptRepository receiptRepository,
                          @Value("${payment.fare.base:2.00}") BigDecimal baseFare,
                          @Value("${payment.fare.per-km:1.50}") BigDecimal perKmRate,
                          @Value("${payment.fare.per-minute:0.25}") BigDecimal perMinuteRate) {
        this.paymentRepository = paymentRepository;
        this.receiptRepository = receiptRepository;
        this.baseFare = baseFare;
        this.perKmRate = perKmRate;
        this.perMinuteRate = perMinuteRate;
    }

    public FareCalculationResponse calculateFare(FareCalculationRequest request) {
        BigDecimal roundedBaseFare = money(baseFare);
        BigDecimal distanceCharge = money(request.distanceKm().multiply(perKmRate));
        BigDecimal durationCharge = money(perMinuteRate.multiply(BigDecimal.valueOf(request.durationMinutes())));
        BigDecimal total = roundedBaseFare.add(distanceCharge).add(durationCharge);
        return new FareCalculationResponse(request.rideId(), roundedBaseFare, distanceCharge,
                durationCharge, total);
    }

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        if (paymentRepository.findByRideId(request.rideId()).isPresent()) {
            throw new InvalidPaymentException("A payment already exists for ride %d".formatted(request.rideId()));
        }
        FareCalculationResponse fare = calculateFare(new FareCalculationRequest(
                request.rideId(), request.distanceKm(), request.durationMinutes()));
        Payment payment = new Payment(request.rideId(), fare.totalFare(),
                request.paymentMethod(), PaymentStatus.PENDING, UUID.randomUUID().toString());
        return PaymentResponse.from(paymentRepository.saveAndFlush(payment));
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(Long paymentId) {
        findPayment(paymentId);
        return receiptRepository.findByPaymentId(paymentId).map(ReceiptResponse::from)
                .orElseThrow(() -> new InvalidPaymentException(
                        "A receipt is available only after payment succeeds"));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long id) {
        return PaymentResponse.from(findPayment(id));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForRide(Long rideId) {
        return paymentRepository.findByRideIdOrderByCreatedAtDesc(rideId).stream()
                .map(PaymentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream().map(PaymentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PaymentSummaryResponse getPaymentSummary() {
        return new PaymentSummaryResponse(
                paymentRepository.count(),
                paymentRepository.countByStatus(PaymentStatus.SUCCESS),
                paymentRepository.countByStatus(PaymentStatus.PENDING),
                paymentRepository.countByStatus(PaymentStatus.REFUNDED),
                paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS));
    }

    @Transactional
    public PaymentResponse updateStatus(Long id, PaymentStatus nextStatus) {
        Payment payment = findPayment(id);
        boolean allowed = (payment.getStatus() == PaymentStatus.PENDING
                && (nextStatus == PaymentStatus.SUCCESS || nextStatus == PaymentStatus.FAILED))
                || (payment.getStatus() == PaymentStatus.SUCCESS && nextStatus == PaymentStatus.REFUNDED);
        if (!allowed) {
            throw new InvalidPaymentException("Cannot change payment status from %s to %s"
                    .formatted(payment.getStatus(), nextStatus));
        }
        payment.setStatus(nextStatus);
        Payment saved = paymentRepository.save(payment);
        if (nextStatus == PaymentStatus.SUCCESS) {
            receiptRepository.findByPaymentId(id).orElseGet(() -> receiptRepository.save(new Receipt(saved)));
        }
        return PaymentResponse.from(saved);
    }

    private Payment findPayment(Long id) {
        return paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
