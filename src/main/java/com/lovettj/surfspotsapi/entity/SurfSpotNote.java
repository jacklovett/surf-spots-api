package com.lovettj.surfspotsapi.entity;

import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.Tide;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;

@Entity
@Table(name = "surf_spot_note", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "surf_spot_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurfSpotNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "surf_spot_id", nullable = false)
    private SurfSpot surfSpot;

    @Size(max = 10000)
    @Column(nullable = true, length = 10000)
    private String noteText;

    // Optional structured fields
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Tide preferredTide;

    @Size(max = 100)
    @Column(length = 100, nullable = true)
    private String preferredSwellDirection;

    @Size(max = 100)
    @Column(length = 100, nullable = true)
    private String preferredWind;

    @Size(max = 100)
    @Column(length = 100, nullable = true)
    private String preferredSwellRange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private SkillLevel skillRequirement;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime modifiedAt;
}

