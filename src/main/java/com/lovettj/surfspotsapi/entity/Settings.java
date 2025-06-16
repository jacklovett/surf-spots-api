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
}