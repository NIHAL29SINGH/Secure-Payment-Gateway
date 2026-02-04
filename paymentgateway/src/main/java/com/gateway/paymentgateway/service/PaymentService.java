package com.gateway.paymentgateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.paymentgateway.entity.*;
import com.gateway.paymentgateway.repository.PaymentRepository;
import com.gateway.paymentgateway.repository.UserKycRepository;
import com.gateway.paymentgateway.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    private static final String IDEM_PREFIX = "idem:";

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepo;
    private final UserRepository userRepo;
    private final UserKycRepository kycRepo;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Counter paymentSuccessCounter;
    private final Counter paymentFailureCounter;

    @Value("${razorpay.key.id}")
    private String razorpayKey;

    public PaymentService(
            RazorpayClient razorpayClient,
            PaymentRepository paymentRepo,
            UserRepository userRepo,
            UserKycRepository kycRepo,
            RedisTemplate<String, String> redisTemplate,
            MeterRegistry meterRegistry
    ) {
        this.razorpayClient = razorpayClient;
        this.paymentRepo = paymentRepo;
        this.userRepo = userRepo;
        this.kycRepo = kycRepo;
        this.redisTemplate = redisTemplate;

        this.paymentSuccessCounter =
                meterRegistry.counter("payments.success");
        this.paymentFailureCounter =
                meterRegistry.counter("payments.failure");
    }

    // =========================================
    // 🔐 STATE MACHINE GUARD
    // =========================================
    private void updateStatus(Payment payment, PaymentStatus next) {
        PaymentStateMachine.validate(payment.getStatus(), next);
        payment.setStatus(next);
        paymentRepo.save(payment);
    }

    // =========================================
    // ✅ FIX: FETCH PAYMENT BY ORDER ID
    // =========================================
    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepo.findByRazorpayOrderId(orderId);
    }

    // =========================================
    // ✅ IDEMPOTENT CREATE PAYMENT
    // =========================================
    public Map<String, Object> createPayment(
            String email,
            Double amount,
            String idempotencyKey
    ) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new RuntimeException("Idempotency-Key header is required");
        }

        String redisKey = IDEM_PREFIX + idempotencyKey;

        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(
                        cached,
                        new TypeReference<>() {}
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to read idempotent cache", e);
            }
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
            options.put("amount", (int) (amount * 100));
            options.put("currency", "INR");
            options.put("receipt", "rcpt_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(options);

            Payment payment = new Payment();
            payment.setUser(user);
            payment.setAmount(amount);
            payment.setCurrency("INR");
            payment.setRazorpayOrderId(order.get("id"));
            payment.setStatus(PaymentStatus.CREATED);
            payment.setCreatedAt(LocalDateTime.now());
            payment.setIdempotencyKey(idempotencyKey);

            paymentRepo.save(payment);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", order.get("amount"));
            response.put("currency", "INR");
            response.put("razorpayKey", razorpayKey);
            response.put("idempotent", false);

            redisTemplate.opsForValue().set(
                    redisKey,
                    objectMapper.writeValueAsString(response),
                    Duration.ofMinutes(15)
            );

            return response;

        } catch (Exception e) {
            paymentFailureCounter.increment();
            throw new RuntimeException("Razorpay order creation failed", e);
        }
    }

    // =========================================
    // ✅ WEBHOOK SUCCESS
    // =========================================
    public void markPaymentSuccess(String orderId, String paymentId) {

        Payment payment = paymentRepo.findByRazorpayOrderId(orderId);

        if (payment == null) {
            throw new RuntimeException("Payment not found");
        }

        payment.setRazorpayPaymentId(paymentId);

        updateStatus(payment, PaymentStatus.CAPTURED);
        updateStatus(payment, PaymentStatus.SUCCESS);

        paymentSuccessCounter.increment();
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

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new RuntimeException("Only SUCCESS payments can be refunded");
        }

        updateStatus(payment, PaymentStatus.REFUND_REQUESTED);

        payment.setRefundStatus(RefundStatus.REQUESTED);
        payment.setRefundRequestedAt(LocalDateTime.now());

        paymentRepo.save(payment);
    }

    // =========================================
    // ✅ ADMIN APPROVE & REFUND
    // =========================================
    public void approveAndRefund(Long paymentId) {

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.REFUND_REQUESTED) {
            throw new RuntimeException("Refund not requested");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("amount", (int) (payment.getAmount() * 100));

            razorpayClient.payments.refund(
                    payment.getRazorpayPaymentId(),
                    options
            );

            updateStatus(payment, PaymentStatus.REFUNDED);

            payment.setRefundStatus(RefundStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());

            paymentRepo.save(payment);

        } catch (Exception e) {
            throw new RuntimeException("Refund failed", e);
        }
    }

    public String getRazorpayKey() {
        return razorpayKey;
    }
}
