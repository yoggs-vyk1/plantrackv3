package com.plantrack.backend.controller;

import com.plantrack.backend.dto.*;
import com.plantrack.backend.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    // Get Dashboard Statistics
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(analyticsService.getDashboardStats());
    }

    // Get Analytics for a specific User
    @GetMapping("/users/{userId}/analytics")
    public ResponseEntity<AnalyticsDTO> getUserAnalytics(@PathVariable Long userId) {
        return ResponseEntity.ok(analyticsService.getUserAnalytics(userId));
    }

    // Get Departmental Insights (All authenticated users for gamification)
    @GetMapping("/analytics/departmental-insights")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<DepartmentalInsightsDTO>> getDepartmentalInsights() {
        return ResponseEntity.ok(analyticsService.getDepartmentalInsights());
    }

    // Get Velocity Metrics for a specific user
    @GetMapping("/analytics/velocity/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<VelocityMetricsDTO> getUserVelocity(@PathVariable Long userId) {
        return ResponseEntity.ok(analyticsService.getUserVelocity(userId));
    }

    // Get Velocity Metrics for all users (Admin only)
    @GetMapping("/analytics/velocity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VelocityMetricsDTO>> getAllUsersVelocity() {
        return ResponseEntity.ok(analyticsService.getAllUsersVelocity());
    }

    // ============================================================
    // GAMIFICATION ENDPOINTS
    // ============================================================

    // Get gamified velocity metrics with filtering and sorting
    @GetMapping("/analytics/gamified-velocity")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<GamifiedVelocityDTO>> getGamifiedVelocity(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Double minCompletionRate,
            @RequestParam(required = false) Double maxCompletionRate,
            @RequestParam(required = false) Integer minTasks,
            @RequestParam(required = false) Integer maxTasks,
            @RequestParam(required = false) String performanceTier,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
        return ResponseEntity.ok(analyticsService.getAllGamifiedVelocity(
                department, search, minCompletionRate, maxCompletionRate,
                minTasks, maxTasks, performanceTier, sortBy, sortOrder));
    }

    // Get performance score for a specific user
    @GetMapping("/analytics/performance-score/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PerformanceScoreDTO> getPerformanceScore(
            @PathVariable Long userId,
            @RequestParam(required = false) String department) {
        return ResponseEntity.ok(analyticsService.calculatePerformanceScore(userId, department));
    }

    // Get badges for a specific user
    @GetMapping("/analytics/badges/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<BadgeDTO>> getUserBadges(@PathVariable Long userId) {
        return ResponseEntity.ok(analyticsService.calculateBadges(userId));
    }

    // Get leaderboard
    @GetMapping("/analytics/leaderboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<LeaderboardEntryDTO>> getLeaderboard(
            @RequestParam(required = false, defaultValue = "OVERALL") String metricType,
            @RequestParam(required = false) String department,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        return ResponseEntity.ok(analyticsService.getLeaderboard(metricType, department, limit));
    }

    // Get all departments
    @GetMapping("/analytics/departments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<String>> getAllDepartments() {
        return ResponseEntity.ok(analyticsService.getAllDepartments());
    }

    // Get gamified velocity for a specific user
    @GetMapping("/analytics/gamified-velocity/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<GamifiedVelocityDTO> getUserGamifiedVelocity(
            @PathVariable Long userId,
            @RequestParam(required = false) String department) {
        return ResponseEntity.ok(analyticsService.getGamifiedVelocity(userId, department));
    }
}