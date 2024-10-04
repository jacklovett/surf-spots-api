package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.Continent;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContinentRepository extends JpaRepository<Continent, Long> {
  Optional<Continent> findBySlug(String slug);
}