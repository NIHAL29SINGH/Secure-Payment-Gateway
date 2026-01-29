package com.gateway.paymentgateway.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;

    private boolean emailVerified;
    private boolean googleVerified;
    private boolean active;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private Role role;





    }


