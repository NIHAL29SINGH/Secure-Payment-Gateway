package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.dto.request.LoginRequest;
import com.gateway.paymentgateway.dto.request.SetPasswordRequest;
import com.gateway.paymentgateway.dto.response.AuthResponse;
import com.gateway.paymentgateway.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    public String googleSignup(@RequestBody Map<String, String> req) {
        authService.googleSignup(req.get("idToken"));
        return "Verification email sent";
    }

    @PostMapping("/verify")
    public String verify(@RequestParam String token,
                         @RequestBody Map<String, String> body) {
        authService.verifyEmail(token, body.get("password"));
        return "Account verified successfully";
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }
}
