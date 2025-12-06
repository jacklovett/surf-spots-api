package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.TripInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripInvitationRepository extends JpaRepository<TripInvitation, String> {
    List<TripInvitation> findByEmail(String email);
    List<TripInvitation> findByTripId(String tripId);
    Optional<TripInvitation> findByToken(String token);
    Optional<TripInvitation> findByTripIdAndEmail(String tripId, String email);
}




