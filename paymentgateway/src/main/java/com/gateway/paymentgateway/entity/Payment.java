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

    private String status; // CREATED, SUCCESS, FAILED

    private String receiver; // who received payment

    private LocalDateTime createdAt;

    @ManyToOne
    private User user;
}
