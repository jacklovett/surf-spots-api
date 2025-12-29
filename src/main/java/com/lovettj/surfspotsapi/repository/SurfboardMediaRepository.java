package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.SurfboardMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SurfboardMediaRepository extends JpaRepository<SurfboardMedia, String> {
    @Query("SELECT sm FROM SurfboardMedia sm WHERE sm.surfboard.id = :surfboardId ORDER BY sm.createdAt ASC")
    List<SurfboardMedia> findBySurfboardId(@Param("surfboardId") String surfboardId);
}

