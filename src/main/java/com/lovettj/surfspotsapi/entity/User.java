package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
  @Id
  @Column(length = 36)
  private String id;

  @PrePersist
  public void generateId() {
    this.id = UUID.randomUUID().toString();
  }

  @Column(unique = true)
  private String email;

  private String name;
  private String password;
  private String providerId;

  @Enumerated(EnumType.STRING)
  private AuthProvider provider;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "settings_id")
  private Settings settings;

  private String country;
  private String city;

  @OneToMany(mappedBy = "user")
  private List<UserSurfSpot> userSurfSpots;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  private LocalDateTime modifiedAt;
}
