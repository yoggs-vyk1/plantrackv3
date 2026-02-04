package com.plantrack.backend.service;

import com.plantrack.backend.model.User;
import com.plantrack.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Loading user by username: email={}", email);
        
        // 1. Find the user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found: email={}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });
        if (user == null) {
            logger.error("User is null after repository lookup: email={}", email);
            throw new UsernameNotFoundException("User not found");
        }

        // 2. Convert to Spring Security's "UserDetails" format
        // We map your "MANAGER" role to Spring's "ROLE_MANAGER" format
        String role = "ROLE_" + user.getRole();
        logger.debug("User loaded successfully: email={}, userId={}, role={}", email, user.getUserId(), role);
        
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority(role))
        );
    }
}