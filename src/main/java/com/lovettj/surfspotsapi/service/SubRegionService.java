package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SubRegion;
import com.lovettj.surfspotsapi.repository.RegionRepository;
import com.lovettj.surfspotsapi.repository.SubRegionRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubRegionService {
  private final SubRegionRepository subRegionRepository;
  private final RegionRepository regionRepository;

  public SubRegionService(SubRegionRepository subRegionRepository, RegionRepository regionRepository) {
    this.subRegionRepository = subRegionRepository;
    this.regionRepository = regionRepository;
  }

  public SubRegion getSubRegionBySlug(String slug) {
    return subRegionRepository.findBySlug(slug).orElseThrow(() -> new EntityNotFoundException("SubRegion not found"));
  }

  public List<SubRegion> getSubRegionsByRegion(Long regionId) {
    return subRegionRepository.findByRegionId(regionId);
  }

  public List<SubRegion> findSubRegionsByRegionSlug(String slug) {
    Region region = regionRepository.findBySlug(slug)
        .orElseThrow(() -> new EntityNotFoundException("Region not found"));
    return subRegionRepository.findByRegion(region);
  }
}




