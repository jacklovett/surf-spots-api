package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import lombok.*;

@Entity
@Table(name = "trip_media")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripMedia {
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
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "media_type", nullable = false, length = 50)
    private String mediaType;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;
}