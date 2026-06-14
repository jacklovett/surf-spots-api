package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "surf_event_contest_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurfEventContestDetail {

    @Id
    @Column(name = "surf_event_id")
    private Long surfEventId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "surf_event_id")
    private SurfEvent surfEvent;

    @Column(nullable = false)
    private String organizer;

    @Column
    private String series;

    @Column(name = "season_year", nullable = false)
    private Integer seasonYear;

    @Column(name = "venue_location_key", nullable = false)
    private String venueLocationKey;
}
