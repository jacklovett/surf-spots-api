package com.lovettj.surfspotsapi.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores only the HASH of a password reset token, never the plaintext.
 * The plaintext is emailed to the user and never touches the database.
 * Matches OWASP ASVS V2.5 (reset tokens must be hashed like any other secret).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    public PasswordResetToken(String tokenHash, User user) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
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
