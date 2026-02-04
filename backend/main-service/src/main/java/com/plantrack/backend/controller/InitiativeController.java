package com.plantrack.backend.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plantrack.backend.model.Initiative;
import com.plantrack.backend.service.InitiativeService;

@RestController
@RequestMapping("/api")
public class InitiativeController {

    private static final Logger logger = LoggerFactory.getLogger(InitiativeController.class);

    @Autowired
    private InitiativeService initiativeService;

    @PostMapping("/milestones/{milestoneId}/initiatives")
    public Initiative createInitiative(@PathVariable Long milestoneId, 
                                       @RequestParam(required = false) String assignedUserIds,
                                       @RequestBody Initiative initiative) {
        // Support both new format (comma-separated IDs) and legacy format (single userId)
        List<Long> userIds;
        if (assignedUserIds != null && !assignedUserIds.trim().isEmpty()) {
            // Parse comma-separated user IDs
            userIds = Arrays.stream(assignedUserIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } else {
            // Fallback: try to get from request body if provided
            // This maintains backward compatibility
            throw new RuntimeException("assignedUserIds parameter is required. Provide comma-separated user IDs (e.g., ?assignedUserIds=1,2,3)");
        }
        
        return initiativeService.createInitiative(milestoneId, userIds, initiative);
    }

    @GetMapping("/milestones/{milestoneId}/initiatives")
    public List<Initiative> getInitiatives(@PathVariable Long milestoneId) {
        return initiativeService.getInitiativesByMilestone(milestoneId);
    }

    // NEW: Update Endpoint
    @PutMapping("/initiatives/{initiativeId}")
    public Initiative updateInitiative(@PathVariable Long initiativeId, @RequestBody Initiative initiative) {
        return initiativeService.updateInitiative(initiativeId, initiative);
    }

    // Get all initiatives assigned to a user
    @GetMapping("/users/{userId}/initiatives")
    public List<Initiative> getMyInitiatives(@PathVariable Long userId) {
        logger.debug("GET /users/{}/initiatives - Request received", userId);
        List<Initiative> initiatives = initiativeService.getInitiativesByUser(userId);
        logger.info("GET /users/{}/initiatives - Found {} initiatives", userId, initiatives.size());
        return initiatives;
    }
}