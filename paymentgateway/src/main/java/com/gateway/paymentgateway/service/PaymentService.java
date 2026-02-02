package com.gateway.paymentgateway.service;

import com.gateway.paymentgateway.entity.*;
import com.gateway.paymentgateway.repository.*;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepo;
    private final UserRepository userRepo;
    private final UserKycRepository kycRepo;

    // =========================================
    // ✅ CREATE PAYMENT (BACKEND ONLY)
    // =========================================
    public Map<String, Object> createPayment(String email, Double amount) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserKyc kyc = kycRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("KYC not submitted"));

        if (kyc.getStatus() != KycStatus.APPROVED) {
            throw new RuntimeException("KYC not approved. Payment not allowed.");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("amount", amount.intValue() * 100); // paise
            options.put("currency", "INR");
            options.put("receipt", "rcpt_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(options);

            Payment payment = new Payment();
            payment.setUser(user);
            payment.setAmount(amount);
            payment.setCurrency("INR");
            payment.setRazorpayOrderId(order.get("id"));
            payment.setStatus("CREATED");
            payment.setCreatedAt(LocalDateTime.now());

            paymentRepo.save(payment);

            // ✅ JSON response (frontend/mobile will use this)
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", amount);
            response.put("currency", "INR");
            response.put("status", "CREATED");
            response.put("razorpayKey", "rzp_test_S9Ef1TcXxY5LmI");

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Razorpay payment creation failed", e);
        }
    }

    // =========================================
    // ✅ MARK PAYMENT SUCCESS (WEBHOOK)
    // =========================================
    public void markPaymentSuccess(String orderId, String paymentId) {

        Payment payment =
                paymentRepo.findByRazorpayOrderId(orderId);

        if (payment == null) {
            throw new RuntimeException("Payment not found for orderId: " + orderId);
        }

        payment.setRazorpayPaymentId(paymentId);
        payment.setStatus("SUCCESS");

        paymentRepo.save(payment);
    }
}
