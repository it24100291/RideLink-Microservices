package com.ridelink.payment.repository;

import com.ridelink.payment.entity.Payment;
import com.ridelink.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByRideIdOrderByCreatedAtDesc(Long rideId);

    long countByStatus(PaymentStatus status);

    @Query("select coalesce(sum(payment.amount), 0) from Payment payment where payment.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);
}
