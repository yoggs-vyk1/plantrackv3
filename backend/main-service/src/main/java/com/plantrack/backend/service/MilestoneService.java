package com.plantrack.backend.service;

import com.plantrack.backend.dto.CreateNotificationRequest;
import com.plantrack.backend.feign.NotificationService;
import com.plantrack.backend.model.Initiative;
import com.plantrack.backend.model.Milestone;
import com.plantrack.backend.model.Plan;
import com.plantrack.backend.model.User;
import com.plantrack.backend.repository.InitiativeRepository;
import com.plantrack.backend.repository.MilestoneRepository;
import com.plantrack.backend.repository.PlanRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MilestoneService {

    private static final Logger logger = LoggerFactory.getLogger(MilestoneService.class);
    private static final String STATUS_CANCELLED = "CANCELLED";
    @Autowired
    private MilestoneRepository milestoneRepository;
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private InitiativeRepository initiativeRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private AuditService auditService;
    @PersistenceContext
    private EntityManager entityManager;

    public Milestone createMilestone(Long planId, Milestone milestone) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan (Goal) not found with id: " + planId));

        milestone.setPlan(plan);
        Milestone savedMilestone = milestoneRepository.save(milestone);

        // Audit Log
        auditService.logCreate("MILESTONE", savedMilestone.getMilestoneId(),
                "Created milestone: " + savedMilestone.getTitle() + " in plan: " + plan.getTitle());

        return savedMilestone;
    }

    public List<Milestone> getMilestonesByPlan(Long planId) {
        return milestoneRepository.findByPlanPlanId(planId);
    }

    public Milestone updateMilestone(Long milestoneId, Milestone details) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new RuntimeException("Milestone not found"));

        String oldStatus = milestone.getStatus();

        milestone.setTitle(details.getTitle());
        milestone.setDueDate(details.getDueDate());
        milestone.setCompletionPercent(details.getCompletionPercent());
        milestone.setStatus(details.getStatus());

        Milestone savedMilestone = milestoneRepository.save(milestone);

        // Audit Log
        if (details.getStatus() != null && oldStatus != null && !oldStatus.equals(details.getStatus())) {
            auditService.logStatusChange("MILESTONE", milestoneId, oldStatus, details.getStatus(),
                    "Milestone '" + savedMilestone.getTitle() + "' status changed from " + oldStatus + " to " + details.getStatus());
        } else {
            auditService.logUpdate("MILESTONE", milestoneId, "Updated milestone: " + savedMilestone.getTitle());
        }

        return savedMilestone;
    }

    @Transactional
    public void deleteMilestone(Long milestoneId) {
        // Load milestone with all relationships
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new RuntimeException("Milestone not found"));

        String milestoneTitle = milestone.getTitle();

        // Get all associated initiatives (they are already loaded due to EAGER fetch)
        List<Initiative> initiatives = new ArrayList<>(milestone.getInitiatives());

        // First, delete all initiatives and their relationships
        // This must be done before deleting the milestone to avoid foreign key constraint issues
        for (Initiative initiative : initiatives) {
            Long initiativeId = initiative.getInitiativeId();
            if (initiativeId != null) {
                // Clear ManyToMany relationships first using native SQL to ensure it happens
                entityManager.createNativeQuery("DELETE FROM initiative_assignees WHERE initiative_id = :initiativeId")
                        .setParameter("initiativeId", initiativeId)
                        .executeUpdate();

                // Delete comments using native SQL
                entityManager.createNativeQuery("DELETE FROM comments WHERE initiative_id = :initiativeId")
                        .setParameter("initiativeId", initiativeId)
                        .executeUpdate();

                // Delete the initiative using native SQL
                entityManager.createNativeQuery("DELETE FROM initiatives WHERE initiative_id = :initiativeId")
                        .setParameter("initiativeId", initiativeId)
                        .executeUpdate();
            }
        }

        // Finally delete the milestone using native SQL
        entityManager.createNativeQuery("DELETE FROM milestones WHERE milestone_id = :milestoneId")
                .setParameter("milestoneId", milestoneId)
                .executeUpdate();

        // Flush to ensure all deletions are executed
        entityManager.flush();

        // Audit Log (after successful deletion)
        auditService.logDelete("MILESTONE", milestoneId, "Deleted milestone: " + milestoneTitle);
    }

    /**
     * Cancel a milestone and cascade cancellation to all child initiatives
     *
     * @return Map containing affected counts
     */
    @Transactional
    public Map<String, Object> cancelMilestoneWithCascade(Long milestoneId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new RuntimeException("Milestone not found with id: " + milestoneId));

        if (STATUS_CANCELLED.equals(milestone.getStatus())) {
            throw new RuntimeException("Milestone is already cancelled");
        }

        String oldStatus = milestone.getStatus() != null ? milestone.getStatus() : "PLANNED";
        int initiativeCancelledCount = 0;
        List<Long> notifiedUserIds = new ArrayList<>();

        // Cascade to initiatives
        List<Initiative> initiatives = milestone.getInitiatives();
        for (Initiative initiative : initiatives) {
            if (!STATUS_CANCELLED.equals(initiative.getStatus())) {
                String oldInitiativeStatus = initiative.getStatus();
                initiative.setStatus(STATUS_CANCELLED);
                initiativeRepository.save(initiative);
                initiativeCancelledCount++;

                // Audit log for initiative
                auditService.logStatusChange("INITIATIVE", initiative.getInitiativeId(),
                        oldInitiativeStatus, STATUS_CANCELLED,
                        "Initiative '" + initiative.getTitle() + "' cancelled (cascade from milestone cancellation)");

                // Collect user IDs for notification
                if (initiative.getAssignedUsers() != null) {
                    for (User user : initiative.getAssignedUsers()) {
                        if (!notifiedUserIds.contains(user.getUserId())) {
                            notifiedUserIds.add(user.getUserId());
                        }
                    }
                }
            }
        }

        // Cancel the milestone itself
        milestone.setStatus(STATUS_CANCELLED);
        milestoneRepository.save(milestone);

        // Audit log for milestone
        auditService.logStatusChange("MILESTONE", milestoneId, oldStatus, STATUS_CANCELLED,
                "Milestone '" + milestone.getTitle() + "' cancelled with cascade (" +
                        initiativeCancelledCount + " initiatives affected)");

        // Send notifications to all affected users
        String planTitle = milestone.getPlan() != null ? milestone.getPlan().getTitle() : "Unknown Plan";
        for (Long notifyUserId : notifiedUserIds) {
            try {
                System.out.println("About to call notification service");
                notificationService.createNotification(
                        new CreateNotificationRequest(
                                notifyUserId,
                                "WARNING",
                                "Milestone '" + milestone.getTitle() + "' in plan '" + planTitle +
                                        "' has been cancelled. Your assigned initiatives are now cancelled.",
                                "MILESTONE",
                                milestoneId
                        )
                );
                System.out.println("Notification service is done");
            } catch (Exception e) {
                System.out.println("Notification service not called");
                logger.error("Failed to send cancellation notification: userId={}, milestoneId={}",
                        notifyUserId, milestoneId, e);
            }
        }

        // Return summary
        Map<String, Object> result = new HashMap<>();
        result.put("milestoneId", milestoneId);
        result.put("milestoneTitle", milestone.getTitle());
        result.put("initiativesAffected", initiativeCancelledCount);
        result.put("usersNotified", notifiedUserIds.size());
        return result;
    }

    /**
     * Get cascade cancellation preview for a milestone
     */
    public Map<String, Object> getCancelCascadePreview(Long milestoneId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new RuntimeException("Milestone not found with id: " + milestoneId));

        int activeInitiatives = 0;
        List<String> initiativeNames = new ArrayList<>();

        for (Initiative initiative : milestone.getInitiatives()) {
            if (!STATUS_CANCELLED.equals(initiative.getStatus())) {
                activeInitiatives++;
                initiativeNames.add(initiative.getTitle());
            }
        }

        Map<String, Object> preview = new HashMap<>();
        preview.put("milestoneId", milestoneId);
        preview.put("milestoneTitle", milestone.getTitle());
        preview.put("milestoneStatus", milestone.getStatus());
        preview.put("initiativesCount", activeInitiatives);
        preview.put("initiativeNames", initiativeNames);
        preview.put("isAlreadyCancelled", STATUS_CANCELLED.equals(milestone.getStatus()));
        return preview;
    }
}