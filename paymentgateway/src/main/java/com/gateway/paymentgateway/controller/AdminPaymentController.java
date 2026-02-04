package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.entity.Payment;
import com.gateway.paymentgateway.entity.PaymentStatus;
import com.gateway.paymentgateway.entity.RefundStatus;
import com.gateway.paymentgateway.service.PaymentService;
import com.gateway.paymentgateway.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    // ✅ All payments
    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    // ✅ Payments by user
    @GetMapping("/user/{userId}")
    public List<Payment> getPaymentsByUser(@PathVariable Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    // ✅ Approve refund
    @PostMapping("/refund/{paymentId}")
    public String approveRefund(@PathVariable Long paymentId) {
        paymentService.approveAndRefund(paymentId);
        return "Refund processed successfully";
    }


    }


