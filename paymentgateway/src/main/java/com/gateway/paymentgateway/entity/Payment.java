package com.gateway.paymentgateway.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String razorpayOrderId;
    private String razorpayPaymentId;

    private Double amount;
    private String currency;

    private String status; // CREATED, SUCCESS, REFUNDED

    @Enumerated(EnumType.STRING)
    private RefundStatus refundStatus; // REQUESTED, APPROVED, REFUNDED

    private LocalDateTime refundRequestedAt;
    private LocalDateTime refundedAt;

    @ManyToOne
    private User user;

    private LocalDateTime createdAt;
}
