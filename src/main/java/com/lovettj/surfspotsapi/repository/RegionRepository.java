package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {
  Optional<Region> findBySlug(String slug);

  List<Region> findByCountryId(Long countryId);

  List<Region> findByCountry(Country country);
}
