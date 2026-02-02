package com.gateway.paymentgateway.service;

import com.gateway.paymentgateway.entity.KycStatus;
import com.gateway.paymentgateway.entity.Payment;
import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.repository.PaymentRepository;
import com.gateway.paymentgateway.repository.UserKycRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepo;
    private final UserKycRepository kycRepo;

    // ✅ CREATE PAYMENT ORDER
    public Payment createOrder(User user, Double amount) {

        // 🔐 KYC CHECK
        var kyc = kycRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("KYC not submitted"));

        if (kyc.getStatus() != KycStatus.APPROVED) {
            throw new RuntimeException("KYC not approved. Payment not allowed.");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("amount", amount * 100);
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

            return paymentRepo.save(payment);

        } catch (Exception e) {
            throw new RuntimeException("Payment creation failed");
        }
    }

    // ✅ VERIFY PAYMENT (used by webhook)
    public void markPaymentSuccess(String orderId, String paymentId) {

        Payment payment = paymentRepo.findByRazorpayOrderId(orderId);

        if (payment == null)
            throw new RuntimeException("Payment not found");

        payment.setRazorpayPaymentId(paymentId);
        payment.setStatus("SUCCESS");

        paymentRepo.save(payment);
    }
}
