package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.Surfboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SurfboardRepository extends JpaRepository<Surfboard, String> {
    @Query("SELECT s FROM Surfboard s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<Surfboard> findByUserId(@Param("userId") String userId);

    @Query("SELECT s FROM Surfboard s WHERE s.id = :id AND s.user.id = :userId")
    Optional<Surfboard> findByIdAndUserId(@Param("id") String id, @Param("userId") String userId);
}
