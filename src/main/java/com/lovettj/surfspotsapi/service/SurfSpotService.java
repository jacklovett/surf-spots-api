package com.lovettj.surfspotsapi.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;

@Service
public class SurfSpotService {
  private final SurfSpotRepository surfSpotRepository;

  public SurfSpotService(SurfSpotRepository surfSpotRepository) {
    this.surfSpotRepository = surfSpotRepository;
  }

  public List<SurfSpot> getAllSurfSpots() {
    return surfSpotRepository.findAll();
  }

  public Optional<SurfSpot> getSurfSpotById(Long id) {
    return surfSpotRepository.findById(id);
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