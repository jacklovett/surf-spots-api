package com.lovettj.surfspotsapi.entity;

import com.lovettj.surfspotsapi.enums.CrowdLevel;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.Tide;
import com.lovettj.surfspotsapi.enums.WaveQuality;
import com.lovettj.surfspotsapi.enums.WaveSize;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

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

    @Column(name = "session_notes", columnDefinition = "TEXT")
    private String sessionNotes;

    private Boolean wouldSurfAgain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "surfboard_id")
    private Surfboard surfboard;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
