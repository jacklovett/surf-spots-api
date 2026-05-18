package com.lovettj.surfspotsapi.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores only the SHA-256 digest of an email verification token (plaintext is emailed once).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class EmailVerificationToken {

    public EmailVerificationToken(String tokenHash, User user) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = Instant.now().plus(48, ChronoUnit.HOURS);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;
}
