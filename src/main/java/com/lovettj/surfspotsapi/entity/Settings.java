package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private boolean newSurfSpotEmails;
    private boolean nearbySurfSpotsEmails;
    private boolean swellSeasonEmails;
    private boolean eventEmails;
    private boolean promotionEmails;

    /** Display preference: {@code metric} or {@code imperial}. */
    @Column(name = "preferred_units", nullable = false)
    @Builder.Default
    private String preferredUnits = "metric";

    @Column(name = "last_known_latitude")
    private Double lastKnownLatitude;

    @Column(name = "last_known_longitude")
    private Double lastKnownLongitude;

    @Column(name = "last_known_location_at")
    private java.time.Instant lastKnownLocationAt;
}