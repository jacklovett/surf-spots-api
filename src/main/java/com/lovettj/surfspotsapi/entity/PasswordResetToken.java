package com.lovettj.surfspotsapi.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
public class PasswordResetToken {

    public PasswordResetToken(String token, User user) {
        this.token = token;
        this.user = user;
        this.expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;
}
