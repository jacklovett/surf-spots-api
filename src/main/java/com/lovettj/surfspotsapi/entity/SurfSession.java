package com.lovettj.surfspotsapi.entity;

import com.lovettj.surfspotsapi.enums.CrowdLevel;
import com.lovettj.surfspotsapi.enums.ExternalSessionProvider;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.Tide;
import com.lovettj.surfspotsapi.enums.WaveQuality;
import com.lovettj.surfspotsapi.enums.WaveSize;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Logged surf session: spot, skill, conditions, and timeline ({@link #sessionStartInstant} / {@link #sessionEndInstant}).
 *
 * <p>Vendor-only metrics (heart rate, device IDs, partner scores) are not stored here unless the product
 * explicitly models them; do not pack that into {@link #sessionNotes}. When a contract requires them, add
 * dedicated nullable columns, a child entity, or a small extension table - only what is needed.
 */
@Entity
@Table(name = "surf_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurfSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "surf_spot_id", nullable = false)
    private SurfSpot surfSpot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillLevel skillLevel;

    /** Local calendar day at the surf spot (denormalized from {@link #sessionStartInstant} for queries and labels). */
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    /**
     * Minutes between {@link #sessionStartInstant} and {@link #sessionEndInstant} when both are set;
     * denormalized for sorting and analytics.
     */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /**
     * Session window on the timeline (same model as wearables and forecast/partner APIs: UTC instants).
     * Local clock labels for the UI are derived at read time using {@link SurfSpot#getIanaZoneId()}.
     */
    @Column(name = "session_start_instant")
    private Instant sessionStartInstant;

    @Column(name = "session_end_instant")
    private Instant sessionEndInstant;

    /**
     * Integration source for {@link #externalSessionId}. Omitted with id for sessions logged only in our UI.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "external_session_provider")
    private ExternalSessionProvider externalSessionProvider;

    /**
     * Provider-local stable id for idempotent imports. Unique per user together with {@link #externalSessionProvider}.
     */
    @Column(name = "external_session_id")
    private String externalSessionId;

    @Enumerated(EnumType.STRING)
    private WaveSize waveSize;

    @Enumerated(EnumType.STRING)
    private CrowdLevel crowdLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "wave_quality")
    private WaveQuality waveQuality;

    @Column(name = "swell_direction")
    private String swellDirection;

    @Column(name = "wind_direction")
    private String windDirection;

    @Enumerated(EnumType.STRING)
    private Tide tide;

    /**
     * Free text the user enters in the app. Not for structured partner payloads, device dumps, or JSON blobs;
     * those belong in dedicated storage when the product requires them.
     */
    @Column(name = "session_notes", columnDefinition = "TEXT")
    private String sessionNotes;

    private Boolean wouldSurfAgain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "surfboard_id")
    private Surfboard surfboard;

    @OneToMany(mappedBy = "surfSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SurfSessionMedia> media;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
