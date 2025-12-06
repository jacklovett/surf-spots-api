package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trip_invitation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripInvitation {
    @Id
    @Column(length = 36)
    private String id;

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private String email;

    @ManyToOne
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @Column(nullable = false)
    private LocalDateTime invitedAt;

    @Column
    private LocalDateTime acceptedAt;

    @Column(nullable = false)
    private String status; // PENDING, ACCEPTED, DECLINED, EXPIRED

    @Column
    private String token; // Unique token for invitation link

    // Optional link to TripMember when invitation is accepted (for audit trail)
    @OneToOne
    @JoinColumn(name = "trip_member_id")
    private TripMember tripMember;
}




