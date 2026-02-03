package com.gateway.paymentgateway.service;

import com.gateway.paymentgateway.entity.KycStatus;
import com.gateway.paymentgateway.entity.Payment;
import com.gateway.paymentgateway.entity.RefundStatus;
import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.entity.UserKyc;
import com.gateway.paymentgateway.repository.PaymentRepository;
import com.gateway.paymentgateway.repository.UserKycRepository;
import com.gateway.paymentgateway.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${razorpay.key.id}")
    private String razorpayKey;

    // =========================================
    // ✅ CREATE PAYMENT / ORDER
    // =========================================
    public Map<String, Object> createPayment(String email, Double amount) {

        if (amount == null || amount <= 0) {
            throw new RuntimeException("Invalid payment amount");
        }

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserKyc kyc = kycRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("KYC not submitted"));

        if (kyc.getStatus() != KycStatus.APPROVED) {
            throw new RuntimeException("KYC not approved");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("amount", (int) (amount * 100)); // paise
            options.put("currency", "INR");
            options.put("receipt", "rcpt_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(options);

            Payment payment = new Payment();
            payment.setUser(user);
            payment.setAmount(amount); // rupees
            payment.setCurrency("INR");
            payment.setRazorpayOrderId(order.get("id"));
            payment.setStatus("CREATED");
            payment.setCreatedAt(LocalDateTime.now());

            paymentRepo.save(payment);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", order.get("amount")); // paise
            response.put("currency", "INR");
            response.put("razorpayKey", razorpayKey);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Razorpay order creation failed", e);
        }
    }

    // =========================================
    // ✅ FIND PAYMENT BY ORDER ID
    // =========================================
    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepo.findByRazorpayOrderId(orderId);
    }

    // =========================================
    // ✅ WEBHOOK: PAYMENT SUCCESS
    // =========================================
    public void markPaymentSuccess(String orderId, String paymentId) {

        Payment payment = paymentRepo.findByRazorpayOrderId(orderId);

        if (payment == null) {
            throw new RuntimeException("Payment not found for orderId: " + orderId);
        }

        payment.setRazorpayPaymentId(paymentId);
        payment.setStatus("SUCCESS");

        paymentRepo.save(payment);
    }

    // =========================================
    // ✅ USER REQUEST REFUND
    // =========================================
    public void requestRefund(Long paymentId, String email) {

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (!payment.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized refund request");
        }

        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new RuntimeException("Only successful payments can be refunded");
        }

        payment.setRefundStatus(RefundStatus.REQUESTED);
        payment.setRefundRequestedAt(LocalDateTime.now());

        paymentRepo.save(payment);

        System.out.println("REFUND REQUESTED for payment ID: " + paymentId);
    }

    // =========================================
    // ✅ ADMIN APPROVE & REFUND
    // =========================================
    public void approveAndRefund(Long paymentId) {

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getRefundStatus() != RefundStatus.REQUESTED) {
            throw new RuntimeException("Refund not requested");
        }

        if (payment.getRazorpayPaymentId() == null) {
            throw new RuntimeException("Payment not captured yet");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("amount", (int) (payment.getAmount() * 100));

            Refund refund = razorpayClient.payments
                    .refund(payment.getRazorpayPaymentId(), options);

            payment.setRefundStatus(RefundStatus.REFUNDED);
            payment.setStatus("REFUNDED");
            payment.setRefundedAt(LocalDateTime.now());

            paymentRepo.save(payment);

        } catch (Exception e) {
            throw new RuntimeException("Refund failed", e);
        }
    }

    // =========================================
    // ✅ HELPERS
    // =========================================
    public String getRazorpayKey() {
        return razorpayKey;
    }

    public Map<String, Object> createOrder(String email, Double amount) {
        return createPayment(email, amount);
    }
}
