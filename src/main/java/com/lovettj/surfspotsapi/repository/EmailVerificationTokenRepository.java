package com.lovettj.surfspotsapi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lovettj.surfspotsapi.entity.EmailVerificationToken;
import com.lovettj.surfspotsapi.entity.User;

import jakarta.transaction.Transactional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user = :user")
    void deleteByUser(@Param("user") User user);
}
