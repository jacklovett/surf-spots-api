package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CountryRepository extends JpaRepository<Country, Long> {
  Optional<Country> findBySlug(String slug);

  List<Country> findByContinent(Continent continent);

  List<Country> findByContinentOrderByNameAsc(Continent continent);

  @Query("SELECT c FROM Country c LEFT JOIN c.continent cont ORDER BY cont.name, c.name")
  List<Country> findAllByOrderByContinentNameAscNameAsc();

  /**
   * Find country by name (case-insensitive, for matching Mapbox country names)
   */
  Optional<Country> findByNameIgnoreCase(String name);
}
