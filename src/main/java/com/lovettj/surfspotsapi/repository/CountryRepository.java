package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, Long> {
  Optional<Country> findBySlug(String slug);

  List<Country> findByContinent(Continent continent);
}
