package com.plantrack.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired; // Import for Autowired
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Import for DELETE/PUT rules
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

@Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 1. PUBLIC ACCESS
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                // =========================================================
                // 2. SPECIFIC EXCEPTIONS (MUST COME BEFORE GENERIC RULES!)
                // =========================================================
                
                // Allow Managers to Create Plans under a User ID
                // This MUST be before "/api/users/**"
                .requestMatchers("/api/users/*/plans/**").hasAnyRole("MANAGER", "ADMIN")
                // Allow Employees to get their assigned plans
                .requestMatchers(HttpMethod.GET, "/api/users/*/assigned-plans").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                // Allow all authenticated users to GET users for mentions (comment @mentions)
                .requestMatchers(HttpMethod.GET, "/api/users/mentions").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                // Allow Managers and Admins to GET full users list (for assigning initiatives)
                .requestMatchers(HttpMethod.GET, "/api/users").hasAnyRole("MANAGER", "ADMIN")
                
                // Allow Employees, Managers, and Admins to GET their own initiatives
                .requestMatchers(HttpMethod.GET, "/api/users/*/initiatives").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                
                // Allow Employees, Managers, and Admins to GET their assigned plans
                .requestMatchers(HttpMethod.GET, "/api/users/*/assigned-plans").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")

                // =========================================================
                // 3. GENERIC ADMIN RULES (for POST, PUT, DELETE on users)
                // =========================================================
                .requestMatchers("/api/users/**").hasRole("ADMIN")

                // 4. REPORTING
                .requestMatchers("/api/reports/**").hasAnyRole("MANAGER", "ADMIN")

                // 5. PLAN/MILESTONE URLS (Employees can GET plans, but only Managers/Admins can modify)
                .requestMatchers(HttpMethod.GET, "/api/plans/**").hasAnyRole("MANAGER", "EMPLOYEE", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/plans/**", "/api/milestones/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/plans/**", "/api/milestones/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/plans/**", "/api/milestones/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/api/milestones/**").hasAnyRole("MANAGER", "ADMIN")

                // 6. INITIATIVES (Strict Rules)
                .requestMatchers(HttpMethod.DELETE, "/api/initiatives/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/api/initiatives/**").hasAnyRole("MANAGER", "EMPLOYEE", "ADMIN")
                
                // 6.1. COMMENTS (Assigned users, managers, and admins can comment)
                .requestMatchers("/api/initiatives/*/comments").hasAnyRole("MANAGER", "EMPLOYEE", "ADMIN")
                .requestMatchers("/api/comments/**").hasAnyRole("MANAGER", "EMPLOYEE", "ADMIN")

                // 7. DASHBOARD STATS
                .requestMatchers("/api/dashboard/**").hasAnyRole("MANAGER", "EMPLOYEE", "ADMIN")
                
                // 8. ANALYTICS (All authenticated users - Employees, Managers, and Admins)
                .requestMatchers("/api/analytics/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                
                // 9. AUDIT LOGS (Admin only)
                .requestMatchers("/api/audit-logs/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}