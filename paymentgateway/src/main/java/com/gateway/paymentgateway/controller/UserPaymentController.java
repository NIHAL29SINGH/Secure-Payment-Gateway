package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.entity.Payment;
import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/payments")
@RequiredArgsConstructor
public class UserPaymentController {

    private final PaymentRepository paymentRepo;

    @GetMapping
    public List<Payment> getMyPayments(
            @AuthenticationPrincipal User user
    ) {
        return paymentRepo.findByUserId(user.getId());
    }
}
