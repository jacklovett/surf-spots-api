package com.lovettj.surfspotsapi.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.service.ContinentService;

@RestController
@RequestMapping("/api/continents")
public class ContinentController {

  private final ContinentService continentService;

  public ContinentController(ContinentService continentService) {
    this.continentService = continentService;
  }

  @GetMapping
  public ResponseEntity<List<Continent>> getContinents() {
    List<Continent> continents = continentService.getContinents();
    return ResponseEntity.ok(continents);
  }

  @GetMapping("/{continentSlug}")
  public ResponseEntity<Continent> getContinentBySlug(@PathVariable String continentSlug) {
    Continent continent = continentService.getContinentBySlug(continentSlug);
    return ResponseEntity.ok(continent);
  }
}
