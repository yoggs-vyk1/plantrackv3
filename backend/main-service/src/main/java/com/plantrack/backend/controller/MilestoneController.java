package com.plantrack.backend.controller;

import com.plantrack.backend.model.Milestone;
import com.plantrack.backend.service.MilestoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MilestoneController {

    @Autowired
    private MilestoneService milestoneService;

    // Create a Milestone for a specific Plan (Goal)
    @PostMapping("/plans/{planId}/milestones")
    public Milestone createMilestone(@PathVariable Long planId, @RequestBody Milestone milestone) {
        return milestoneService.createMilestone(planId, milestone);
    }

    // Get all Milestones for a Plan
    @GetMapping("/plans/{planId}/milestones")
    public List<Milestone> getMilestonesByPlan(@PathVariable Long planId) {
        return milestoneService.getMilestonesByPlan(planId);
    }

    // Update Milestone Progress
    @PutMapping("/milestones/{milestoneId}")
    public Milestone updateMilestone(@PathVariable Long milestoneId, @RequestBody Milestone details) {
        return milestoneService.updateMilestone(milestoneId, details);
    }
    
    // Delete Milestone
    @DeleteMapping("/milestones/{milestoneId}")
    public ResponseEntity<Void> deleteMilestone(@PathVariable Long milestoneId) {
        try {
            milestoneService.deleteMilestone(milestoneId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete milestone: " + e.getMessage(), e);
        }
    }

    /**
     * Get preview of cascade cancellation for a milestone
     */
    @GetMapping("/milestones/{milestoneId}/cancel-preview")
    public ResponseEntity<Map<String, Object>> getCancelPreview(@PathVariable Long milestoneId) {
        return ResponseEntity.ok(milestoneService.getCancelCascadePreview(milestoneId));
    }

    /**
     * Cancel a milestone with cascade to all initiatives
     */
    @PostMapping("/milestones/{milestoneId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelMilestoneWithCascade(@PathVariable Long milestoneId) {
        return ResponseEntity.ok(milestoneService.cancelMilestoneWithCascade(milestoneId));
    }
}