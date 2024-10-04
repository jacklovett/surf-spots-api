package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.service.CountryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {
  private final CountryService countryService;

  public CountryController(CountryService countryService) {
    this.countryService = countryService;
  }

  @GetMapping("/{countrySlug}")
  public ResponseEntity<Country> getCountryBySlug(@PathVariable String countrySlug) {
    Country country = countryService.getCountryBySlug(countrySlug);
    return ResponseEntity.ok(country);
  }

  @GetMapping("/continent/{continentSlug}")
  public ResponseEntity<List<Country>> getCountriesByContinent(@PathVariable String continentSlug) {
    List<Country> countries = countryService.getCountriesByContinent(continentSlug);
    return ResponseEntity.ok(countries);
  }
}
