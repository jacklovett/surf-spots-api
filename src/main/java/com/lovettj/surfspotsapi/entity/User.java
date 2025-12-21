package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.lovettj.surfspotsapi.enums.SkillLevel;

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
  
  @Min(13)
  @Max(120)
  private Integer age;
  
  private String gender;
  
  @Min(50) // 50 cm (metric min) or 20 inches = 51 cm (imperial min, but metric min is lower)
  @Max(305) // 120 inches = 305 cm (imperial max after conversion)
  private Integer height; // stored in cm
  
  @Min(9) // 20 lbs = 9 kg (imperial min after conversion)
  @Max(500) // 500 kg (metric max)
  private Integer weight; // stored in kg
  
  @Enumerated(EnumType.STRING)
  private SkillLevel skillLevel;

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
