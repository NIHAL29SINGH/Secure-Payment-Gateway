package com.gateway.paymentgateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // ❌ Disable CSRF (JWT based)
                .csrf(csrf -> csrf.disable())

                // ❌ Stateless session
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 🔐 Authorization rules
                .authorizeHttpRequests(auth -> auth

                        // ✅ Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // ✅ Auth APIs
                        .requestMatchers("/api/auth/**").permitAll()

                        // ✅ Razorpay webhook
                        .requestMatchers("/api/payment/webhook").permitAll()

                        // ✅ Razorpay checkout page (NO JWT)
                        .requestMatchers(
                                "/pay",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()

                        // 🔐 Admin only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 🔐 Everything else requires JWT
                        .anyRequest().authenticated()
                )

                // ✅ JWT filter
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // 🔑 Password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
