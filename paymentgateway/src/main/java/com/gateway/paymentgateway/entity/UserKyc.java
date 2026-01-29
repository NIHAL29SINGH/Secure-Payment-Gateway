package com.gateway.paymentgateway.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_kyc")
@Getter @Setter
public class UserKyc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private User user;

    private String fatherName;
    private LocalDate dob;
    private String address;
    private String panNumber;
    private String bankAccount;
    private String ifsc;

    @Enumerated(EnumType.STRING)
    private KycStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
