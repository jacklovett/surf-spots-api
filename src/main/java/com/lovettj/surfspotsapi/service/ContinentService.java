package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.repository.ContinentRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContinentService {
  private final ContinentRepository continentRepository;

  public ContinentService(ContinentRepository continentRepository) {
    this.continentRepository = continentRepository;
  }

  public List<Continent> getContinents() {
    return continentRepository.findAll();
  }

  public Continent getContinentBySlug(String slug) {
    return continentRepository.findBySlug(slug).orElseThrow(() -> new EntityNotFoundException("Continent not found"));
  }
}
