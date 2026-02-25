package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.Continent;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContinentRepository extends JpaRepository<Continent, Long> {
  Optional<Continent> findBySlug(String slug);

  Optional<Continent> findByNameIgnoreCase(String name);

  List<Continent> findAllByOrderByNameAsc();

  /** Fetches continents with countries only (no regions, sub-regions, or surf spots). */
  @Query("SELECT DISTINCT c FROM Continent c LEFT JOIN FETCH c.countries ORDER BY c.name")
  List<Continent> findAllWithCountriesByOrderByNameAsc();
}