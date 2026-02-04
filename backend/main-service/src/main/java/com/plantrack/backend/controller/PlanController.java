package com.plantrack.backend.controller;

import com.plantrack.backend.model.Plan;
import com.plantrack.backend.service.PlanService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PlanController {

    private static final Logger logger = LoggerFactory.getLogger(PlanController.class);

    @Autowired
    private PlanService planService;

    @PostMapping("/users/{userId}/plans")
    public ResponseEntity<Plan> createPlan(@PathVariable Long userId, @Valid @RequestBody Plan plan) {
        return ResponseEntity.ok(planService.createPlan(userId, plan));
    }

    @GetMapping("/plans")
    public ResponseEntity<Page<Plan>> getAllPlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "planId") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return ResponseEntity.ok(planService.getAllPlans(pageable));
    }

    @GetMapping("/users/{userId}/plans")
    public ResponseEntity<List<Plan>> getPlansByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(planService.getPlansByUserId(userId));
    }

    @GetMapping("/plans/{planId}")
    public ResponseEntity<Plan> getPlanById(@PathVariable Long planId) {
        return ResponseEntity.ok(planService.getPlanById(planId));
    }

    @PutMapping("/plans/{planId}")
    public ResponseEntity<Plan> updatePlan(@PathVariable Long planId, @RequestBody Plan planDetails) {
        return ResponseEntity.ok(planService.updatePlan(planId, planDetails));
    }

    @DeleteMapping("/plans/{planId}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long planId) {
        planService.deletePlan(planId);
        return ResponseEntity.noContent().build();
    }

    // Endpoint for employees to get plans with their assigned initiatives
    @GetMapping("/users/{userId}/assigned-plans")
    public ResponseEntity<List<Plan>> getPlansWithAssignedInitiatives(@PathVariable Long userId) {
        logger.debug("GET /users/{}/assigned-plans - Request received", userId);
        List<Plan> plans = planService.getPlansWithAssignedInitiatives(userId);
        logger.info("GET /users/{}/assigned-plans - Found {} plans with assigned initiatives", userId, plans.size());
        return ResponseEntity.ok(plans);
    }

    /**
     * Get preview of cascade cancellation (counts of affected entities)
     */
    @GetMapping("/plans/{planId}/cancel-preview")
    public ResponseEntity<Map<String, Object>> getCancelPreview(@PathVariable Long planId) {
        return ResponseEntity.ok(planService.getCancelCascadePreview(planId));
    }

    /**
     * Cancel a plan with cascade to all milestones and initiatives
     */
    @PostMapping("/plans/{planId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelPlanWithCascade(@PathVariable Long planId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Get userId from authentication context if needed
        Long userId = null; // Can be extracted from auth if needed for logging
        return ResponseEntity.ok(planService.cancelPlanWithCascade(planId, userId));
    }
}