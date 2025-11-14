package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "regions") // Exclude regions to avoid circular references
public class Country extends SluggableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Size(max = 1000)
    private String description;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "continent_id")
  @JsonBackReference // Matches @JsonManagedReference on Continent.countries
  private Continent continent;

    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL)
    private List<Region> regions;
}
