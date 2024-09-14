package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.Continent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContinentRepository extends JpaRepository<Continent, Long> {
}