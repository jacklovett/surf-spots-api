package com.lovettj.surfspotsapi.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.lovettj.surfspotsapi.entity.User;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
}
