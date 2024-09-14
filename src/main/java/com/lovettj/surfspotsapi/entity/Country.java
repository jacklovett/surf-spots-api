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
public class Country {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String description;

  @ManyToOne
  @JoinColumn(name = "continent_id")
  private Continent continent;

  @OneToMany(mappedBy = "country")
  private List<Region> regions;
}