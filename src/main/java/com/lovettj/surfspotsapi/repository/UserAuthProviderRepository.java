package com.lovettj.surfspotsapi.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.lovettj.surfspotsapi.entity.UserAuthProvider;
import com.lovettj.surfspotsapi.entity.AuthProvider;

public interface UserAuthProviderRepository extends JpaRepository<UserAuthProvider, Long> {
    Optional<UserAuthProvider> findByProviderAndProviderId(AuthProvider provider, String providerId);
    List<UserAuthProvider> findByUserId(String userId);
    boolean existsByUserIdAndProvider(String userId, AuthProvider provider);
} 
