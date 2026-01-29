package com.gateway.paymentgateway.repository;

import com.gateway.paymentgateway.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
