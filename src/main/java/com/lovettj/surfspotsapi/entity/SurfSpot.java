package com.lovettj.surfspotsapi.entity;

import com.lovettj.surfspotsapi.validators.*;
import com.lovettj.surfspotsapi.enums.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

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

    @NotBlank
    private String name;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private WaveDirection waveDirection;

    @Min(0)
    @Column(nullable = true)
    private Double minSurfHeight;

    @Min(0)
    @Column(nullable = true)
    private Double maxSurfHeight;

    @Min(0)
    @Max(5)
    private Integer rating;

    private Double latitude;

    private Double longitude;

    @ManyToOne
    @JoinColumn(name = "region_id")
    @JsonBackReference("region-surfspots")
    private Region region;
    
    @ManyToOne
    @JoinColumn(name = "sub_region_id")
    @JsonBackReference("subregion-surfspots")
    private SubRegion subRegion;    

    @Enumerated(EnumType.STRING)
    private SurfSpotStatus status;

    private Boolean foodNearby;

    @ElementCollection(targetClass = FoodOption.class)
    @CollectionTable(name = "surfspot_food_options", joinColumns = @JoinColumn(name = "surfspot_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "food_option")
    private List<FoodOption> foodOptions;

    private Boolean accommodationNearby;

    @ElementCollection(targetClass = AccommodationOption.class)
    @CollectionTable(name = "surfspot_accommodation_options", joinColumns = @JoinColumn(name = "surfspot_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "accommodation_option")
    private List<AccommodationOption> accommodationOptions;

    @ElementCollection(targetClass = Facility.class)
    @CollectionTable(name = "surfspot_facilities", joinColumns = @JoinColumn(name = "surfspot_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "facility")
    private List<Facility> facilities;

    @ElementCollection(targetClass = Hazard.class)
    @CollectionTable(name = "surfspot_hazards", joinColumns = @JoinColumn(name = "surfspot_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "hazard")
    private List<Hazard> hazards;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Parking parking;

    private Boolean boatRequired;

    @Column(name = "is_wavepool")
    private Boolean isWavepool;

    @Size(max = 500)
    @Column(length = 500)
    private String wavepoolUrl;

    @Size(max = 9)
    private String seasonStart;

    @Size(max = 9)
    private String seasonEnd;

    @ElementCollection
    @Column(name = "forecasts")
    private List<String> forecasts;

    @Column(name = "created_by")
    private String createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime modifiedAt;
}
