package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.entity.Payment;
import com.gateway.paymentgateway.repository.PaymentRepository;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final PaymentRepository paymentRepo;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/webhook")
    public String handleWebhook(HttpServletRequest request,
                                @RequestHeader("X-Razorpay-Signature") String signature) throws Exception {

        String payload = request.getReader()
                .lines()
                .collect(Collectors.joining());

        boolean isValid = Utils.verifyWebhookSignature(
                payload,
                signature,
                webhookSecret
        );

        if (!isValid)
            throw new RuntimeException("Invalid webhook signature");

        JSONObject json = new JSONObject(payload);
        String event = json.getString("event");

        if (event.equals("payment.captured")) {

            JSONObject paymentObj =
                    json.getJSONObject("payload")
                            .getJSONObject("payment")
                            .getJSONObject("entity");

            String orderId = paymentObj.getString("order_id");
            String paymentId = paymentObj.getString("id");

            Payment payment =
                    paymentRepo.findByRazorpayOrderId(orderId);

            if (payment != null) {
                payment.setStatus("SUCCESS");
                payment.setRazorpayPaymentId(paymentId);
                paymentRepo.save(payment);
            }
        }

        return "OK";
    }
}
