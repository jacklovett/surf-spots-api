package com.lovettj.surfspotsapi.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.lovettj.surfspotsapi.enums.EventSource;
import com.lovettj.surfspotsapi.enums.EventStatus;
import com.lovettj.surfspotsapi.enums.EventType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "surf_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurfEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(nullable = false)
    private String name;

    @Column(name = "location_name", nullable = false)
    private String locationName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @ManyToOne
    @JoinColumn(name = "surf_spot_id")
    private SurfSpot surfSpot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventSource source;

    @OneToOne(mappedBy = "surfEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private SurfEventContestDetail contestDetail;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime modifiedAt;

    public void setContestDetail(SurfEventContestDetail contestDetail) {
        this.contestDetail = contestDetail;
        if (contestDetail != null) {
            contestDetail.setSurfEvent(this);
        }
    }
}
