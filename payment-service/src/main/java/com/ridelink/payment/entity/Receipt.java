package com.ridelink.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "receipts")
public class Receipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long paymentId;

    @Column(nullable = false, unique = true)
    private String receiptNumber;

    @Column(nullable = false)
    private Long rideId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String paymentReference;

    @Column(nullable = false, updatable = false)
    private Instant issuedAt;

    protected Receipt() { }

    public Receipt(Payment payment) {
        this.paymentId = payment.getId();
        this.receiptNumber = "R-" + UUID.randomUUID();
        this.rideId = payment.getRideId();
        this.amount = payment.getAmount();
        this.paymentReference = payment.getReferenceId();
    }

    @PrePersist
    void onCreate() { issuedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getPaymentId() { return paymentId; }
    public String getReceiptNumber() { return receiptNumber; }
    public Long getRideId() { return rideId; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentReference() { return paymentReference; }
    public Instant getIssuedAt() { return issuedAt; }
}
