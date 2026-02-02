package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public Object createPayment(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Double amount
    ) {
        // ✅ Spring Security gives UserDetails, NOT your entity
        String email = principal.getUsername();

        return paymentService.createOrder(
                email,
                amount
        );
    }
}
