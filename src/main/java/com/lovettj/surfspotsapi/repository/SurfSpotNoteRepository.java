package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.SurfSpotNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurfSpotNoteRepository extends JpaRepository<SurfSpotNote, Long> {
    Optional<SurfSpotNote> findByUserIdAndSurfSpotId(String userId, Long surfSpotId);
}

