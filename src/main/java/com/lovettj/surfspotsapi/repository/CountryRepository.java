package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, Long> {
}
