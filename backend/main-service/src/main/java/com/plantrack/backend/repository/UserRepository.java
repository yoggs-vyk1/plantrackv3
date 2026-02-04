package com.plantrack.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plantrack.backend.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Add this lookup method
    java.util.Optional<User> findByEmail(String email);
}