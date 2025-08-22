package com.lovettj.surfspotsapi.repository;


import com.lovettj.surfspotsapi.entity.SurfSpot;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for SurfSpot entity.
 * Extends JpaRepository for basic CRUD and SurfSpotRepositoryCustom for dynamic filtering.
 */
public interface SurfSpotRepository extends JpaRepository<SurfSpot, Long>, SurfSpotRepositoryCustom {}
