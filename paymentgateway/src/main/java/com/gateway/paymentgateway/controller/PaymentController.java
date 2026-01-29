package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // Create Razorpay Order
    @PostMapping("/create")
    public Object createPayment(
            @AuthenticationPrincipal User user,
            @RequestParam Double amount
    ) throws Exception {
        return paymentService.createOrder(user, amount);
    }

    // Payment Success Callback
    @PostMapping("/verify")
    public String verifyPayment(
            @RequestParam String orderId,
            @RequestParam String paymentId
    ) {
        paymentService.verifyPayment(orderId, paymentId);
        return "Payment Successful";
    }
}
