package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Continent extends SluggableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @Size(max = 1000)
  @Column(length = 1000)
  private String description;

  @OneToMany(mappedBy = "continent", cascade = CascadeType.ALL)
  @JsonManagedReference("continent-countries")
  private List<Country> countries;
}