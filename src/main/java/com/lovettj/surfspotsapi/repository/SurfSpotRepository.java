package com.lovettj.surfspotsapi.repository;

import java.util.List;
import java.util.Optional;

import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SurfSpotRepository extends JpaRepository<SurfSpot, Long> {

    List<SurfSpot> findByRegion(Region region);

    Optional<SurfSpot> findBySlug(String slug);
}
