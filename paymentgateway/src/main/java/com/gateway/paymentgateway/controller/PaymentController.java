package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
        return paymentService.createOrder(
                (User) principal, // ✔ your CustomUserDetails returns User
                amount
        );
    }
}
