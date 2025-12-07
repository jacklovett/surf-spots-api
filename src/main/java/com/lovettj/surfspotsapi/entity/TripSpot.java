package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import lombok.*;

@Entity
@Table(name = "trip_spot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSpot {
    @Id
    @Column(length = 36)
    private String id;

    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID().toString();
    }

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne
    @JoinColumn(name = "surf_spot_id", nullable = false)
    private SurfSpot surfSpot;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;
}