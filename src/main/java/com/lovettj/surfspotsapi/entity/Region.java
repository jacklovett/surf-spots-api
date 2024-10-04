package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Region extends SluggableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String description;

  @ManyToOne
  @JoinColumn(name = "country_id")
  @JsonBackReference
  private Country country;

  @OneToMany(mappedBy = "region", cascade = CascadeType.ALL)
  private List<SurfSpot> surfSpots;
}