package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import lombok.*;

@Entity
@Table(name = "surfboard_media")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurfboardMedia {
    @Id
    @Column(length = 36)
    private String id;

    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID().toString();
    }

    @ManyToOne
    @JoinColumn(name = "surfboard_id", nullable = false)
    private Surfboard surfboard;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "thumb_url", columnDefinition = "TEXT")
    private String thumbUrl;

    @Column(name = "media_type", nullable = false, length = 50)
    private String mediaType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

