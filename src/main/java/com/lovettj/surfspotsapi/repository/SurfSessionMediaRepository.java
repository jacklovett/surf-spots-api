package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.SurfSessionMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurfSessionMediaRepository extends JpaRepository<SurfSessionMedia, String> {
    List<SurfSessionMedia> findBySurfSessionId(Long surfSessionId);
}
