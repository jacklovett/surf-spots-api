package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SubRegion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubRegionRepository extends JpaRepository<SubRegion, Long> {
  Optional<SubRegion> findBySlug(String slug);

  List<SubRegion> findByRegionId(Long regionId);

  List<SubRegion> findByRegion(Region region);

  @Query("SELECT s FROM SubRegion s LEFT JOIN s.region r ORDER BY r.name, s.name")
  List<SubRegion> findAllByOrderByRegionNameAscNameAsc();
}




