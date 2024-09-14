package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;
import java.util.List;

import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Region {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String description;

  @ManyToOne
  @JoinColumn(name = "country_id")
  private Country country;

  @OneToMany(mappedBy = "region")
  private List<SurfSpot> surfSpots;
}