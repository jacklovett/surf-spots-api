package com.lovettj.surfspotsapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.lovettj.surfspotsapi.entity.User;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);

    @Query("""
            SELECT u FROM User u
            JOIN FETCH u.settings settings
            WHERE settings.newSurfSpotEmails = true
              AND u.emailVerified = true
              AND u.email IS NOT NULL
              AND u.email <> ''
            """)
    List<User> findUsersWithNewSurfSpotEmailsEnabled();

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.settings settings
            WHERE (settings.swellSeasonEmails = true OR settings.eventEmails = true)
              AND u.emailVerified = true
              AND u.email IS NOT NULL
              AND u.email <> ''
            """)
    List<User> findUsersWithWatchListEmailAlertsEnabled();
}
