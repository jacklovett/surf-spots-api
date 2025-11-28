package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.SwellSeason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SwellSeasonRepository extends JpaRepository<SwellSeason, Long> {
    Optional<SwellSeason> findByName(String name);
}

