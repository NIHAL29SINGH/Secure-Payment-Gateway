package com.gateway.paymentgateway.config;

import com.gateway.paymentgateway.entity.Role;
import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class AdminInitializer {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @PostConstruct
    public void createAdmin() {

        if (userRepo.findByEmail(adminEmail).isPresent()) {
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        admin.setEmailVerified(true);
        admin.setGoogleVerified(false);

        userRepo.save(admin);

        System.out.println("✅ Admin account created: " + adminEmail);
    }
}
