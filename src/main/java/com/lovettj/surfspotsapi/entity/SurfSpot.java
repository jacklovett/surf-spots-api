package com.lovettj.surfspotsapi.entity;

import com.lovettj.surfspotsapi.validators.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurfSpot extends SluggableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @NotBlank
    private String name;

    @Enumerated(EnumType.STRING)
    private BeachBottomType beachBottomType;

    @ValidDirection
    @Size(max = 7)
    private String swellDirection;

    @ValidDirection
    @Size(max = 7)
    private String windDirection;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private SurfSpotType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private SkillLevel skillLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Tide tide;

    @ValidSeason
    @Size(max = 21)
    private String season;

    private Double latitude;

    private Double longitude;

    @Min(0)
    @Max(5)
    private Integer rating;

    @Min(0)
    @Column(nullable = true)
    private Double minSurfHeight;

    @Min(0)
    @Column(nullable = true)
    private Double maxSurfHeight;

    @ManyToOne
    @JoinColumn(name = "region_id")
    @JsonBackReference
    private Region region;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime modifiedAt;
}
