package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.SurfboardImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SurfboardImageRepository extends JpaRepository<SurfboardImage, String> {
    @Query("SELECT si FROM SurfboardImage si WHERE si.surfboard.id = :surfboardId ORDER BY si.createdAt ASC")
    List<SurfboardImage> findBySurfboardId(@Param("surfboardId") String surfboardId);
}
