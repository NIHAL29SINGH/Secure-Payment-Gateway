package com.gateway.paymentgateway.repository;

import com.gateway.paymentgateway.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserId(Long userId);

    Payment findByRazorpayOrderId(String orderId);

    // ✅ IDEMPOTENCY
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
