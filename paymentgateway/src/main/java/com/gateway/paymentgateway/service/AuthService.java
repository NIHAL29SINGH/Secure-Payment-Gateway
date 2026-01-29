package com.gateway.paymentgateway.service;

import com.gateway.paymentgateway.dto.request.LoginRequest;
import com.gateway.paymentgateway.entity.EmailVerificationToken;
import com.gateway.paymentgateway.entity.Role;
import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.repository.EmailVerificationTokenRepository;
import com.gateway.paymentgateway.repository.UserRepository;
import com.gateway.paymentgateway.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepo;
    private final EmailVerificationService tokenService;
    private final EmailService emailService;
    private final GoogleOAuthService googleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailVerificationTokenRepository tokenRepo;

    // =========================
    // GOOGLE SIGNUP
    // =========================
    public void googleSignup(String idToken) {

        var payload = googleService.verify(idToken);

        String email = payload.getEmail();
        String name = (String) payload.get("name");

        User user = userRepo.findByEmail(email)
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setName(name);
                    u.setGoogleVerified(true);
                    u.setEmailVerified(false);
                    u.setActive(true);
                    u.setRole(Role.USER);
                    return userRepo.save(u);
                });

        EmailVerificationToken token = tokenService.create(user);

        emailService.send(
                user.getEmail(),
                "Verify your email",
                "Click the link to verify your account:\n" +
                        "http://localhost:8080/api/auth/verify?token=" + token.getToken()
        );
    }

    // =========================
    // VERIFY EMAIL + SET PASSWORD
    // =========================
    public void verifyEmail(String token, String password) {

        EmailVerificationToken verification =
                tokenRepo.findByToken(token)
                        .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (verification.isUsed())
            throw new RuntimeException("Token already used");

        if (verification.getExpiryTime().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Token expired");

        User user = verification.getUser();

        user.setPassword(passwordEncoder.encode(password));
        user.setEmailVerified(true);

        verification.setUsed(true);

        userRepo.save(user);
        tokenRepo.save(verification);
    }

    // =========================
    // LOGIN
    // =========================
    public String login(LoginRequest request) {

        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEmailVerified())
            throw new RuntimeException("Email not verified");

        if (!user.isActive())
            throw new RuntimeException("Account disabled");

        if (user.getPassword() == null)
            throw new RuntimeException("Password not set");

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new RuntimeException("Invalid credentials");

        return jwtUtil.generateToken(user.getEmail());
    }
}
