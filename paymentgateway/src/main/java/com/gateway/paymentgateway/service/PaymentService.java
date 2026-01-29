package com.gateway.paymentgateway.service;

import com.gateway.paymentgateway.entity.Payment;
import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepo;

    @Value("${razorpay.currency}")
    private String currency;

    public Order createOrder(User user, Double amount) throws Exception {

        if (!user.isActive())
            throw new RuntimeException("KYC not approved");

        JSONObject options = new JSONObject();
        options.put("amount", amount * 100); // paisa
        options.put("currency", currency);
        options.put("receipt", "txn_" + System.currentTimeMillis());

        Order order = razorpayClient.orders.create(options);

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setRazorpayOrderId(order.get("id"));
        payment.setStatus("CREATED");
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepo.save(payment);

        return order;
    }

    public void verifyPayment(String orderId, String paymentId) {

        Payment payment = paymentRepo.findByRazorpayOrderId(orderId);

        if (payment == null)
            throw new RuntimeException("Payment not found");

        payment.setRazorpayPaymentId(paymentId);
        payment.setStatus("SUCCESS");

        paymentRepo.save(payment);
    }
}
