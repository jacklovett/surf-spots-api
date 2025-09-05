package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_auth_providers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthProvider {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;
    
    @Column(name = "provider_id", nullable = false)
    private String providerId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private java.time.LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }
} 
