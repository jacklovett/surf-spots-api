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

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "settings_id")
  private Settings settings;

  private String country;
  private String city;

  @OneToMany(mappedBy = "user", orphanRemoval = true)
  private List<UserSurfSpot> userSurfSpots;

  @OneToMany(mappedBy = "user", orphanRemoval = true)
  private List<UserAuthProvider> authProviders;

  @OneToMany(mappedBy = "user", orphanRemoval = true)
  private List<PasswordResetToken> passwordResetTokens;

  @OneToMany(mappedBy = "owner", orphanRemoval = true)
  private List<Trip> trips;

  @OneToMany(mappedBy = "user", orphanRemoval = true)
  private List<Surfboard> surfboards;

  @OneToMany(mappedBy = "user", orphanRemoval = true)
  private List<WatchListSurfSpot> watchListItems;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  private LocalDateTime modifiedAt;
}
