package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SubRegion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubRegionRepository extends JpaRepository<SubRegion, Long> {
  Optional<SubRegion> findBySlug(String slug);

  List<SubRegion> findByRegionId(Long regionId);

  List<SubRegion> findByRegion(Region region);
}




