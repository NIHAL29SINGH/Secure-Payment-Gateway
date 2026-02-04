package com.gateway.paymentgateway.repository;

import com.gateway.paymentgateway.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserId(Long userId);

    Payment findByRazorpayOrderId(String orderId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    // 🔥 IMPORTANT: prevents LazyInitializationException
    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM Payment p
        WHERE p.id = :paymentId
        AND p.user.email = :email
    """)
    boolean existsByIdAndUserEmail(Long paymentId, String email);
}
