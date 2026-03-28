package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.SurfSession;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurfSessionRepository extends JpaRepository<SurfSession, Long> {
    List<SurfSession> findBySurfSpotId(Long surfSpotId);
    List<SurfSession> findBySurfSpotIdAndSkillLevel(Long surfSpotId, SkillLevel skillLevel);
}
