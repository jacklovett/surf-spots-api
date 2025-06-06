package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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

  @Size(max = 1000)
  private String description;

  @ManyToOne
  @JoinColumn(name = "country_id")
  @JsonBackReference
  private Country country;

  @OneToMany(mappedBy = "region", cascade = CascadeType.ALL)
  @JsonManagedReference
  private List<SurfSpot> surfSpots;
}