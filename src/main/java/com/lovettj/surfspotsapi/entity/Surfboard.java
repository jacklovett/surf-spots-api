package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;

@Entity
@Table(name = "surfboards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Surfboard {
    @Id
    @Column(length = 36)
    private String id;

    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID().toString();
    }

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(name = "board_type", length = 50)
    private String boardType;

    @Column(name = "length", precision = 5, scale = 2)
    private BigDecimal length;

    @Column(name = "width", precision = 5, scale = 2)
    private BigDecimal width;

    @Column(name = "thickness", precision = 5, scale = 2)
    private BigDecimal thickness;

    @Column(name = "volume", precision = 6, scale = 2)
    private BigDecimal volume;

    @Column(name = "fin_setup", length = 50)
    private String finSetup;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "model_url", columnDefinition = "TEXT")
    private String modelUrl;

    @OneToMany(mappedBy = "surfboard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SurfboardImage> images;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}