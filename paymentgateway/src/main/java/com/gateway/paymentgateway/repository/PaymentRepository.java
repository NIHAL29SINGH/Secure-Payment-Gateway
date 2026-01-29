package com.gateway.paymentgateway.repository;

import com.gateway.paymentgateway.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserId(Long userId);

    Payment findByRazorpayOrderId(String orderId);
}
