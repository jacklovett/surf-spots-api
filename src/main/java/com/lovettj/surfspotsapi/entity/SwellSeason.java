package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "swell_season")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwellSeason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String name;

    @NotNull
    @Size(max = 9)
    @Column(name = "start_month", nullable = false)
    private String startMonth;

    @NotNull
    @Size(max = 9)
    @Column(name = "end_month", nullable = false)
    private String endMonth;
}

