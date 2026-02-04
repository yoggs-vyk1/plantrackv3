package com.plantrack.backend.service;

import com.plantrack.backend.dto.InitiativeAssignedRequest;
import com.plantrack.backend.dto.StatusUpdateRequest;
import com.plantrack.backend.feign.NotificationService;
import com.plantrack.backend.model.Initiative;
import com.plantrack.backend.model.Milestone;
import com.plantrack.backend.model.User;
import com.plantrack.backend.repository.InitiativeRepository;
import com.plantrack.backend.repository.MilestoneRepository;
import com.plantrack.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InitiativeService {

    private static final Logger logger = LoggerFactory.getLogger(InitiativeService.class);

    @Autowired
    private InitiativeRepository initiativeRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationService notificationService;

    // 1. Create Initiative
    public Initiative createInitiative(Long milestoneId, List<Long> assignedUserIds, Initiative initiative) {
        logger.debug("Creating initiative: milestoneId={}, assignedUserIds={}, title={}",
                milestoneId, assignedUserIds, initiative.getTitle());

        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> {
                    logger.error("Milestone not found: milestoneId={}", milestoneId);
                    return new RuntimeException("Milestone not found");
                });

        // Validate that at least one assignee is provided
        if (assignedUserIds == null || assignedUserIds.isEmpty()) {
            logger.warn("Attempted to create initiative without assignees: milestoneId={}", milestoneId);
            throw new RuntimeException("At least one assignee is required");
        }

        // Validate all users exist and are active
        Set<User> assignedUsers = new HashSet<>();
        for (Long userId : assignedUserIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        logger.error("User not found: userId={}", userId);
                        return new RuntimeException("User not found with ID: " + userId);
                    });
            if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                logger.warn("Attempted to assign inactive user to initiative: userId={}, status={}",
                        userId, user.getStatus());
                throw new RuntimeException("User " + user.getName() + " is not active");
            }
            assignedUsers.add(user);
        }

        initiative.setMilestone(milestone);
        initiative.setAssignedUsers(assignedUsers);

        Initiative savedInitiative = initiativeRepository.save(initiative);
        logger.info("Created initiative: initiativeId={}, title={}, milestoneId={}",
                savedInitiative.getInitiativeId(), savedInitiative.getTitle(), milestoneId);

        // Audit Log - include all assigned users
        String assigneeNames = assignedUsers.stream()
                .map(User::getName)
                .collect(Collectors.joining(", "));
        auditService.logCreate("INITIATIVE", savedInitiative.getInitiativeId(),
                "Created initiative: " + savedInitiative.getTitle() + " in milestone: " + milestone.getTitle() +
                        " assigned to: " + assigneeNames);

        // Notify all assigned users
        for (User user : assignedUsers) {
            try {
                notificationService.notifyInitiativeAssigned(
                        new InitiativeAssignedRequest(
                                user.getUserId(),
                                savedInitiative.getTitle(),
                                savedInitiative.getInitiativeId()
                        )
                );
                logger.info("Successfully sent initiative assignment notification: userId={}, email={}, initiativeId={}, title={}",
                        user.getUserId(), user.getEmail(), savedInitiative.getInitiativeId(), savedInitiative.getTitle());
            } catch (Exception e) {
                logger.error("Failed to send notification to employee: userId={}, email={}, initiativeId={}",
                        user.getUserId(), user.getEmail(), savedInitiative.getInitiativeId(), e);
            }
        }

        // TRIGGER: Recalculate Progress immediately after adding a new task
        updateMilestoneProgress(milestone);

        logger.debug("Completed initiative creation: initiativeId={}", savedInitiative.getInitiativeId());
        return savedInitiative;
    }

    public Initiative updateInitiative(Long id, Initiative updatedData) {
        logger.debug("Updating initiative: initiativeId={}, newStatus={}", id, updatedData.getStatus());

        Initiative initiative = initiativeRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Initiative not found: initiativeId={}", id);
                    return new RuntimeException("Initiative not found");
                });

        String oldStatus = initiative.getStatus();

        // Security check: Employees can only update their own assigned initiatives
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isEmployee = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
        logger.debug("Authorization check: initiativeId={}, isEmployee={}, currentUser={}",
                id, isEmployee, auth.getName());

        if (isEmployee && initiative.getAssignedUsers() != null && !initiative.getAssignedUsers().isEmpty()) {
            // Get current user ID from authentication (assuming email is stored)
            String currentUserEmail = auth.getName();
            User currentUser = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("Current user not found: " + currentUserEmail));

            // Check if the current user is among the assigned users
            boolean isAssigned = initiative.getAssignedUsers().stream()
                    .anyMatch(user -> user.getUserId().equals(currentUser.getUserId()));

            if (!isAssigned) {
                logger.warn("Unauthorized initiative update attempt: initiativeId={}, currentUser={}, assignedUsers={}",
                        id, currentUserEmail, initiative.getAssignedUsers().stream()
                                .map(User::getEmail)
                                .collect(Collectors.joining(", ")));
                throw new RuntimeException("You can only update initiatives assigned to you");
            }

            // Employees can only update status, not title, description, or assigned user
            if (updatedData.getStatus() != null) {
                initiative.setStatus(updatedData.getStatus());
            }
        } else {
            // Managers and Admins can update all fields
            if (updatedData.getTitle() != null && !updatedData.getTitle().isEmpty()) {
                initiative.setTitle(updatedData.getTitle());
            }
            // Description can be null or empty, so always update it
            initiative.setDescription(updatedData.getDescription());
            if (updatedData.getStatus() != null) {
                initiative.setStatus(updatedData.getStatus());
            }

            // Update assigned users if provided (only Managers/Admins)
            if (updatedData.getAssignedUsers() != null && !updatedData.getAssignedUsers().isEmpty()) {
                // Validate all users exist and are active
                Set<User> newAssignedUsers = new HashSet<>();
                for (User userData : updatedData.getAssignedUsers()) {
                    if (userData.getUserId() != null) {
                        User user = userRepository.findById(userData.getUserId())
                                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userData.getUserId()));
                        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                            throw new RuntimeException("User " + user.getName() + " is not active");
                        }
                        newAssignedUsers.add(user);
                    }
                }

                if (newAssignedUsers.isEmpty()) {
                    throw new RuntimeException("At least one assignee is required");
                }

                initiative.setAssignedUsers(newAssignedUsers);
            }
        }

        Initiative savedInitiative = initiativeRepository.save(initiative);

        // Audit Log - Status change
        if (updatedData.getStatus() != null && !oldStatus.equals(updatedData.getStatus())) {
            auditService.logStatusChange("INITIATIVE", id, oldStatus, updatedData.getStatus(),
                    "Initiative '" + savedInitiative.getTitle() + "' status changed from " + oldStatus + " to " + updatedData.getStatus());

            // Notify Manager if employee updated status
            if (isEmployee) {
                String currentUserEmail = auth.getName(); // Declare outside try block for catch block access
                try {
                    // Reload initiative with relationships to ensure milestone and plan are loaded
                    Initiative initiativeWithRelations = initiativeRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Initiative not found after save"));

                    Milestone milestone = initiativeWithRelations.getMilestone();
                    if (milestone != null) {
                        // Reload milestone with plan relationship
                        Milestone milestoneWithPlan = milestoneRepository.findById(milestone.getMilestoneId())
                                .orElseThrow(() -> new RuntimeException("Milestone not found"));

                        if (milestoneWithPlan.getPlan() != null) {
                            User planOwner = milestoneWithPlan.getPlan().getUser();
                            if (planOwner != null) {
                                User currentUserObj = userRepository.findByEmail(currentUserEmail)
                                        .orElseThrow(() -> new RuntimeException("Current user not found: " + currentUserEmail));
                                String employeeName = currentUserObj.getName();
                                notificationService.notifyStatusUpdate(
                                        new StatusUpdateRequest(
                                                planOwner.getUserId(),
                                                employeeName,
                                                savedInitiative.getTitle(),
                                                updatedData.getStatus(),
                                                id
                                        )
                                );
                                logger.info("Successfully sent status update notification to manager: managerId={}, email={}, employeeName={}, initiativeId={}, newStatus={}",
                                        planOwner.getUserId(), planOwner.getEmail(), employeeName, id, updatedData.getStatus());
                            } else {
                                logger.warn("Plan owner is null for initiative: initiativeId={}", id);
                            }
                        } else {
                            logger.warn("Plan is null for milestone: milestoneId={}, initiativeId={}",
                                    milestoneWithPlan.getMilestoneId(), id);
                        }
                    } else {
                        logger.warn("Milestone is null for initiative: initiativeId={}", id);
                    }
                } catch (Exception e) {
                    logger.error("Failed to notify manager about status update: initiativeId={}, employeeEmail={}",
                            id, currentUserEmail, e);
                }
            }
        }

        // Audit Log - General update
        if (updatedData.getTitle() != null || updatedData.getDescription() != null) {
            auditService.logUpdate("INITIATIVE", id, "Updated initiative: " + savedInitiative.getTitle());
        }

        // Audit Log - Assignment change
        if (updatedData.getAssignedUsers() != null && !updatedData.getAssignedUsers().isEmpty()) {
            // Get old assignee IDs (handle null case)
            Set<Long> oldAssigneeIds = new HashSet<>();
            if (initiative.getAssignedUsers() != null && !initiative.getAssignedUsers().isEmpty()) {
                oldAssigneeIds = initiative.getAssignedUsers().stream()
                        .map(User::getUserId)
                        .filter(userId -> userId != null)
                        .collect(Collectors.toSet());
            }

            Set<Long> newAssigneeIds = updatedData.getAssignedUsers().stream()
                    .map(User::getUserId)
                    .filter(userId -> userId != null)
                    .collect(Collectors.toSet());

            // Check if assignment changed
            if (!oldAssigneeIds.equals(newAssigneeIds)) {
                String oldAssigneeNames = "Unassigned";
                if (initiative.getAssignedUsers() != null && !initiative.getAssignedUsers().isEmpty()) {
                    oldAssigneeNames = initiative.getAssignedUsers().stream()
                            .map(User::getName)
                            .filter(name -> name != null)
                            .collect(Collectors.joining(", "));
                    if (oldAssigneeNames.isEmpty()) {
                        oldAssigneeNames = "Unassigned";
                    }
                }

                String newAssigneeNames = savedInitiative.getAssignedUsers().stream()
                        .map(User::getName)
                        .filter(name -> name != null)
                        .collect(Collectors.joining(", "));

                auditService.logUpdate("INITIATIVE", id,
                        "Reassigned initiative from [" + oldAssigneeNames +
                                "] to [" + newAssigneeNames + "]");

                // Notify newly assigned users (users in new set but not in old set)
                Set<Long> newlyAssignedIds = new HashSet<>(newAssigneeIds);
                newlyAssignedIds.removeAll(oldAssigneeIds);

                for (Long newUserId : newlyAssignedIds) {
                    try {
                        notificationService.notifyInitiativeAssigned(
                                new InitiativeAssignedRequest(
                                        newUserId,
                                        savedInitiative.getTitle(),
                                        id
                                )
                        );
                        logger.info("Successfully sent notification to newly assigned user: userId={}, initiativeId={}, title={}",
                                newUserId, id, savedInitiative.getTitle());
                    } catch (Exception e) {
                        logger.error("Failed to notify newly assigned user: userId={}, initiativeId={}",
                                newUserId, id, e);
                    }
                }
            }
        }

        updateMilestoneProgress(initiative.getMilestone());

        logger.info("Updated initiative: initiativeId={}, title={}, statusChanged={}",
                id, savedInitiative.getTitle(), updatedData.getStatus() != null && !oldStatus.equals(updatedData.getStatus()));
        logger.debug("Completed initiative update: initiativeId={}", id);
        return savedInitiative;
    }

    // 3. Get Initiatives
    public List<Initiative> getInitiativesByMilestone(Long milestoneId) {
        logger.debug("Fetching initiatives for milestone: milestoneId={}", milestoneId);
        List<Initiative> initiatives = initiativeRepository.findByMilestoneMilestoneId(milestoneId);
        logger.debug("Found {} initiatives for milestone: milestoneId={}", initiatives.size(), milestoneId);
        return initiatives;
    }

    // 4. Get Initiatives assigned to a user
    public List<Initiative> getInitiativesByUser(Long userId) {
        logger.debug("Fetching initiatives for user: userId={}", userId);
        List<Initiative> initiatives = initiativeRepository.findByAssignedUserUserId(userId);
        logger.info("Found {} initiatives for user: userId={}", initiatives.size(), userId);
        if (!initiatives.isEmpty() && logger.isDebugEnabled()) {
            logger.debug("First initiative milestone: initiativeId={}, milestoneTitle={}",
                    initiatives.get(0).getInitiativeId(),
                    initiatives.get(0).getMilestone() != null ? initiatives.get(0).getMilestone().getTitle() : "null");
        }
        return initiatives;
    }

    // --- AUTOMATION LOGIC ---
    private void updateMilestoneProgress(Milestone milestone) {
        logger.debug("Updating milestone progress: milestoneId={}, title={}",
                milestone.getMilestoneId(), milestone.getTitle());

        // Fetch all sibling initiatives
        List<Initiative> initiatives = initiativeRepository.findByMilestoneMilestoneId(milestone.getMilestoneId());

        if (initiatives.isEmpty()) {
            milestone.setCompletionPercent(0.0);
            milestone.setStatus("PLANNED");
            logger.debug("Milestone has no initiatives, set to PLANNED: milestoneId={}", milestone.getMilestoneId());
        } else {
            long completedCount = initiatives.stream()
                    .filter(i -> "COMPLETED".equalsIgnoreCase(i.getStatus()))
                    .count();

            double percent = ((double) completedCount / initiatives.size()) * 100;
            milestone.setCompletionPercent(percent);

            // Auto-update Milestone Status based on %
            String oldStatus = milestone.getStatus();
            if (percent == 100.0) {
                milestone.setStatus("COMPLETED");
            } else if (percent > 0) {
                milestone.setStatus("IN_PROGRESS");
            } else {
                milestone.setStatus("PLANNED");
            }

            logger.debug("Milestone progress updated: milestoneId={}, completionPercent={}, completedCount={}, totalCount={}, status={}->{}",
                    milestone.getMilestoneId(), percent, completedCount, initiatives.size(), oldStatus, milestone.getStatus());
        }

        // Save the updated Milestone stats
        milestoneRepository.save(milestone);
        logger.debug("Saved milestone progress update: milestoneId={}", milestone.getMilestoneId());
    }
}