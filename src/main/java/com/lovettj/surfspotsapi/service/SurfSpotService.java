package com.lovettj.surfspotsapi.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.repository.RegionRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SurfSpotService {
  private final SurfSpotRepository surfSpotRepository;
  private final RegionRepository regionRepository;

  public SurfSpotService(SurfSpotRepository surfSpotRepository, RegionRepository regionRepository) {
    this.surfSpotRepository = surfSpotRepository;
    this.regionRepository = regionRepository;
  }

  public List<SurfSpot> getAllSurfSpots() {
    return surfSpotRepository.findAll();
  }

  public List<SurfSpot> findSurfSpotsByRegionSlug(String slug) {
    Region region = regionRepository.findBySlug(slug)
        .orElseThrow(() -> new EntityNotFoundException("Region not found"));
    return surfSpotRepository.findByRegion(region);
  }

  public Optional<SurfSpot> getSurfSpotById(Long id) {
    return surfSpotRepository.findById(id);
  }

  public Optional<SurfSpot> findBySlug(String slug) {
    return surfSpotRepository.findBySlug(slug);
  }

  public SurfSpot createSurfSpot(SurfSpot surfSpot) {
    return surfSpotRepository.save(surfSpot);
  }

  public SurfSpot updateSurfSpot(Long id, SurfSpot updatedSurfSpot) {
    if (surfSpotRepository.existsById(id)) {
      updatedSurfSpot.setId(id);
      return surfSpotRepository.save(updatedSurfSpot);
    } else {
      throw new RuntimeException("SurfSpot not found");
    }
  }

  public void deleteSurfSpot(Long id) {
    surfSpotRepository.deleteById(id);
  }
}