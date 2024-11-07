package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;

@Entity
@Table(name = "app_user") // Rename table to avoid conflict with reserved keyword
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = true)
  private String name;

  private String email;

  @Column(nullable = true)
  private String password;

  @Column(nullable = true)
  private String country;

  @Column(nullable = true)
  private String city;

  @Enumerated(EnumType.STRING)
  private AuthProvider provider; // Use enum for provider

  @Column(nullable = true) // Make nullable for email sign-ups
  private String providerId; // Unique ID from the provider

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  private LocalDateTime modifiedAt;

  @OneToMany(mappedBy = "user")
  private List<UserSurfSpot> userSurfSpots;
}
