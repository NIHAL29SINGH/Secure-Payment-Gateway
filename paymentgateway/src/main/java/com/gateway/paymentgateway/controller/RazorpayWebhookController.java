package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.service.PaymentService;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final PaymentService paymentService;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/webhook")
    public String handleWebhook(HttpServletRequest request,
                                @RequestHeader("X-Razorpay-Signature") String signature)
            throws Exception {

        String payload = request.getReader()
                .lines()
                .collect(Collectors.joining());

        boolean valid = Utils.verifyWebhookSignature(
                payload,
                signature,
                webhookSecret
        );

        if (!valid)
            throw new RuntimeException("Invalid webhook");

        JSONObject json = new JSONObject(payload);
        String event = json.getString("event");

        if ("payment.captured".equals(event)) {

            JSONObject entity =
                    json.getJSONObject("payload")
                            .getJSONObject("payment")
                            .getJSONObject("entity");

            String orderId = entity.getString("order_id");
            String paymentId = entity.getString("id");

            paymentService.markPaymentSuccess(orderId, paymentId);
        }

        return "OK";
    }
}
