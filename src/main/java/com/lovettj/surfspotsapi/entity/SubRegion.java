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
public class SubRegion extends SluggableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @Size(max = 1000)
  private String description;

  @ManyToOne
  @JoinColumn(name = "region_id")
  @JsonBackReference
  private Region region;

  @OneToMany(mappedBy = "subRegion", cascade = CascadeType.ALL)
  @JsonManagedReference("surfSpot-subRegion")
  private List<SurfSpot> surfSpots;
}

