package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.repository.CountryRepository;
import com.lovettj.surfspotsapi.repository.RegionRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionService {
  private final CountryRepository countryRepository;
  private final RegionRepository regionRepository;

  public RegionService(RegionRepository regionRepository, CountryRepository countryRepository) {
    this.countryRepository = countryRepository;
    this.regionRepository = regionRepository;
  }

  public Region getRegionBySlug(String slug) {
    return regionRepository.findBySlug(slug).orElseThrow(() -> new EntityNotFoundException("Region not found"));
  }

  public List<Region> getRegionsByCountry(Long countryId) {
    return regionRepository.findByCountryId(countryId);
  }

  public List<Region> findRegionsByCountrySlug(String slug) {
    Country country = countryRepository.findBySlug(slug)
        .orElseThrow(() -> new EntityNotFoundException("Country not found"));
    return regionRepository.findByCountry(country);
  }
}
