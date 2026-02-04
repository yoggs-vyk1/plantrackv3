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
import com.plantrack.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlanService {

    private static final Logger logger = LoggerFactory.getLogger(PlanService.class);
    private static final String STATUS_CANCELLED = "CANCELLED";
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MilestoneRepository milestoneRepository;
    @Autowired
    private InitiativeRepository initiativeRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private AuditService auditService;

    // 1. Logic to Create a Plan linked to a User + Trigger Notification
    public Plan createPlan(Long userId, Plan plan) {
        logger.debug("Creating plan: userId={}, title={}, priority={}",
                userId, plan.getTitle(), plan.getPriority());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found: userId={}", userId);
                    return new RuntimeException("User not found with id: " + userId);
                });

        plan.setUser(user);

        // Set default start date if missing
        if (plan.getStartDate() == null) {
            plan.setStartDate(LocalDateTime.now());
            logger.debug("Set default start date for plan: userId={}", userId);
        }

        Plan savedPlan = planRepository.save(plan);
        logger.info("Created plan: planId={}, title={}, userId={}, priority={}",
                savedPlan.getPlanId(), savedPlan.getTitle(), userId, savedPlan.getPriority());

        // Audit Log
        auditService.logCreate("PLAN", savedPlan.getPlanId(),
                "Created plan: " + savedPlan.getTitle() + " (Priority: " + savedPlan.getPriority() + ")");

        // --- TRIGGER NOTIFICATION ---
        // Automatically alert the user that a plan was assigned
        try {
            notificationService.createNotification(
                    new CreateNotificationRequest(
                            userId,
                            "INFO",
                            "New Plan Created: '" + savedPlan.getTitle() + "'.",
                            "PLAN",
                            savedPlan.getPlanId()
                    )
            );
            logger.debug("Successfully sent plan creation notification: userId={}, planId={}",
                    userId, savedPlan.getPlanId());
        } catch (Exception e) {
            // Don't fail plan creation if notification fails
            logger.error("Failed to create notification for plan creation: userId={}, planId={}",
                    userId, savedPlan.getPlanId(), e);
        }

        logger.debug("Completed plan creation: planId={}", savedPlan.getPlanId());
        return savedPlan;
    }

    // 2. Logic to Get All Plans with Pagination
    public Page<Plan> getAllPlans(Pageable pageable) {
        logger.debug("Fetching all plans with pagination: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        Page<Plan> plans = planRepository.findAll(pageable);
        logger.debug("Found {} plans (total: {})", plans.getNumberOfElements(), plans.getTotalElements());
        return plans;
    }

    // 3. Logic to Get Plans by User ID
    public List<Plan> getPlansByUserId(Long userId) {
        logger.debug("Fetching plans for user: userId={}", userId);
        List<Plan> plans = planRepository.findByUserUserId(userId);
        logger.info("Found {} plans for user: userId={}", plans.size(), userId);
        return plans;
    }

    // 4. Logic to Get Plan by ID
    public Plan getPlanById(Long planId) {
        logger.debug("Fetching plan by ID: planId={}", planId);
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> {
                    logger.error("Plan not found: planId={}", planId);
                    return new RuntimeException("Plan not found with id: " + planId);
                });
        logger.debug("Retrieved plan: planId={}, title={}", planId, plan.getTitle());
        return plan;
    }

    // 5. Logic to Update Plan
    public Plan updatePlan(Long planId, Plan planDetails) {
        logger.debug("Updating plan: planId={}, newStatus={}, newPriority={}",
                planId, planDetails.getStatus(), planDetails.getPriority());

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> {
                    logger.error("Plan not found: planId={}", planId);
                    return new RuntimeException("Plan not found with id: " + planId);
                });

        String oldStatus = plan.getStatus() != null ? plan.getStatus().toString() : null;
        String oldPriority = plan.getPriority() != null ? plan.getPriority().toString() : null;

        plan.setTitle(planDetails.getTitle());
        plan.setDescription(planDetails.getDescription());
        plan.setPriority(planDetails.getPriority());
        plan.setStatus(planDetails.getStatus());
        plan.setStartDate(planDetails.getStartDate());
        plan.setEndDate(planDetails.getEndDate());

        Plan savedPlan = planRepository.save(plan);

        // Audit Log
        if (planDetails.getStatus() != null && oldStatus != null && !oldStatus.equals(planDetails.getStatus().toString())) {
            logger.info("Plan status changed: planId={}, title={}, status={}->{}",
                    planId, savedPlan.getTitle(), oldStatus, planDetails.getStatus());
            auditService.logStatusChange("PLAN", planId, oldStatus, planDetails.getStatus().toString(),
                    "Plan '" + savedPlan.getTitle() + "' status changed from " + oldStatus + " to " + planDetails.getStatus());
        } else {
            logger.info("Plan updated: planId={}, title={}", planId, savedPlan.getTitle());
            auditService.logUpdate("PLAN", planId, "Updated plan: " + savedPlan.getTitle());
        }

        logger.debug("Completed plan update: planId={}", planId);
        return savedPlan;
    }

    /**
     * Cancel a plan and cascade cancellation to all child milestones and initiatives
     *
     * @return Map containing affected counts for confirmation
     */
    @Transactional
    public Map<String, Object> cancelPlanWithCascade(Long planId, Long userId) {
        logger.info("Initiating plan cancellation with cascade: planId={}, userId={}", planId, userId);

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> {
                    logger.error("Plan not found for cancellation: planId={}", planId);
                    return new RuntimeException("Plan not found with id: " + planId);
                });

        if (STATUS_CANCELLED.equals(plan.getStatus() != null ? plan.getStatus().toString() : null)) {
            logger.warn("Attempted to cancel already cancelled plan: planId={}", planId);
            throw new RuntimeException("Plan is already cancelled");
        }

        String oldStatus = plan.getStatus() != null ? plan.getStatus().toString() : "PLANNED";

        // Get all milestones and initiatives for cascade
        List<Milestone> milestones = plan.getMilestones();
        int milestoneCancelledCount = 0;
        int initiativeCancelledCount = 0;
        List<Long> notifiedUserIds = new ArrayList<>();

        // Cascade to milestones and their initiatives
        for (Milestone milestone : milestones) {
            if (!STATUS_CANCELLED.equals(milestone.getStatus())) {
                String oldMilestoneStatus = milestone.getStatus();
                milestone.setStatus(STATUS_CANCELLED);
                milestoneRepository.save(milestone);
                milestoneCancelledCount++;

                // Audit log for milestone
                auditService.logStatusChange("MILESTONE", milestone.getMilestoneId(),
                        oldMilestoneStatus, STATUS_CANCELLED,
                        "Milestone '" + milestone.getTitle() + "' cancelled (cascade from plan cancellation)");
            }

            // Cascade to initiatives
            for (Initiative initiative : milestone.getInitiatives()) {
                if (!STATUS_CANCELLED.equals(initiative.getStatus())) {
                    String oldInitiativeStatus = initiative.getStatus();
                    initiative.setStatus(STATUS_CANCELLED);
                    initiativeRepository.save(initiative);
                    initiativeCancelledCount++;

                    // Audit log for initiative
                    auditService.logStatusChange("INITIATIVE", initiative.getInitiativeId(),
                            oldInitiativeStatus, STATUS_CANCELLED,
                            "Initiative '" + initiative.getTitle() + "' cancelled (cascade from plan cancellation)");

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
        }

        // Cancel the plan itself
        plan.setStatus(com.plantrack.backend.model.PlanStatus.CANCELLED);
        planRepository.save(plan);

        // Audit log for plan
        auditService.logStatusChange("PLAN", planId, oldStatus, STATUS_CANCELLED,
                "Plan '" + plan.getTitle() + "' cancelled with cascade (" +
                        milestoneCancelledCount + " milestones, " + initiativeCancelledCount + " initiatives affected)");

        // Send notifications to all affected users
        for (Long notifyUserId : notifiedUserIds) {
            try {
                notificationService.createNotification(
                        new CreateNotificationRequest(
                                notifyUserId,
                                "WARNING",
                                "Plan '" + plan.getTitle() + "' has been cancelled. All your assigned initiatives under this plan are now cancelled.",
                                "PLAN",
                                planId
                        )
                );
            } catch (Exception e) {
                logger.error("Failed to send cancellation notification to user: userId={}, planId={}",
                        notifyUserId, planId, e);
            }
        }

        // Notify plan owner
        if (plan.getUser() != null && !notifiedUserIds.contains(plan.getUser().getUserId())) {
            try {
                notificationService.createNotification(
                        new CreateNotificationRequest(
                                plan.getUser().getUserId(),
                                "WARNING",
                                "Your plan '" + plan.getTitle() + "' has been cancelled.",
                                "PLAN",
                                planId
                        )
                );
                logger.debug("Sent cancellation notification to plan owner: userId={}, planId={}",
                        plan.getUser().getUserId(), planId);
            } catch (Exception e) {
                logger.error("Failed to send cancellation notification to plan owner: userId={}, planId={}",
                        plan.getUser().getUserId(), planId, e);
            }
        }

        // Return summary
        Map<String, Object> result = new HashMap<>();
        result.put("planId", planId);
        result.put("planTitle", plan.getTitle());
        result.put("milestonesAffected", milestoneCancelledCount);
        result.put("initiativesAffected", initiativeCancelledCount);
        result.put("usersNotified", notifiedUserIds.size());

        logger.info("Plan cancellation completed: planId={}, milestonesAffected={}, initiativesAffected={}, usersNotified={}",
                planId, milestoneCancelledCount, initiativeCancelledCount, notifiedUserIds.size());
        return result;
    }

    /**
     * Get cascade cancellation preview (counts of affected entities)
     */
    public Map<String, Object> getCancelCascadePreview(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found with id: " + planId));

        List<Milestone> milestones = plan.getMilestones();
        int activeMilestones = 0;
        int activeInitiatives = 0;
        List<String> milestoneNames = new ArrayList<>();
        List<String> initiativeNames = new ArrayList<>();

        for (Milestone milestone : milestones) {
            if (!STATUS_CANCELLED.equals(milestone.getStatus())) {
                activeMilestones++;
                milestoneNames.add(milestone.getTitle());
            }
            for (Initiative initiative : milestone.getInitiatives()) {
                if (!STATUS_CANCELLED.equals(initiative.getStatus())) {
                    activeInitiatives++;
                    initiativeNames.add(initiative.getTitle());
                }
            }
        }

        Map<String, Object> preview = new HashMap<>();
        preview.put("planId", planId);
        preview.put("planTitle", plan.getTitle());
        preview.put("planStatus", plan.getStatus() != null ? plan.getStatus().toString() : null);
        preview.put("milestonesCount", activeMilestones);
        preview.put("initiativesCount", activeInitiatives);
        preview.put("milestoneNames", milestoneNames);
        preview.put("initiativeNames", initiativeNames);
        preview.put("isAlreadyCancelled", STATUS_CANCELLED.equals(plan.getStatus() != null ? plan.getStatus().toString() : null));
        return preview;
    }

    // 6. Logic to Delete Plan
    public void deletePlan(Long planId) {
        logger.info("Deleting plan: planId={}", planId);

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> {
                    logger.error("Plan not found for deletion: planId={}", planId);
                    return new RuntimeException("Plan not found with id: " + planId);
                });

        String planTitle = plan.getTitle();
        planRepository.deleteById(planId);

        // Audit Log
        auditService.logDelete("PLAN", planId, "Deleted plan: " + planTitle);
        logger.info("Plan deleted: planId={}, title={}", planId, planTitle);
    }

    // 7. Get Plans that contain initiatives assigned to a specific user (for Employees)
    public List<Plan> getPlansWithAssignedInitiatives(Long userId) {
        logger.debug("Fetching plans with assigned initiatives for user: userId={}", userId);
        // Use custom query with JOINs to efficiently find plans with assigned initiatives
        List<Plan> plans = planRepository.findPlansWithAssignedInitiatives(userId);
        logger.info("Found {} plans with assigned initiatives for user: userId={}", plans.size(), userId);
        return plans;
    }
}