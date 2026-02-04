package com.plantrack.backend.service;

import com.plantrack.backend.dto.CreateNotificationRequest;
import com.plantrack.backend.feign.NotificationService;
import com.plantrack.backend.model.Comment;
import com.plantrack.backend.model.Initiative;
import com.plantrack.backend.model.User;
import com.plantrack.backend.repository.CommentRepository;
import com.plantrack.backend.repository.InitiativeRepository;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);
    // Pattern to match @mentions: @username or @email (extracts username from email)
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)(?:@[\\w.-]+\\.[\\w]+)?", Pattern.CASE_INSENSITIVE);
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private InitiativeRepository initiativeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuditService auditService;
    @Autowired
    private NotificationService notificationService;

    /**
     * Create a new comment with mention processing
     */
    public Comment createComment(Long initiativeId, Comment comment) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = auth.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + currentUserEmail));

        // Load initiative and verify access
        Initiative initiative = initiativeRepository.findById(initiativeId)
                .orElseThrow(() -> new RuntimeException("Initiative not found"));

        // Authorization: Check if user can comment (assigned users, managers, admins)
        if (!canComment(initiative, currentUser)) {
            throw new RuntimeException("You don't have permission to comment on this initiative");
        }

        // Set relationships
        comment.setInitiative(initiative);
        comment.setAuthor(currentUser);
        comment.setDeleted(false);

        // Process mentions
        Set<User> mentionedUsers = processMentions(comment.getContent(), initiative, currentUser);
        comment.setMentionedUsers(mentionedUsers);

        // Save comment
        Comment savedComment = commentRepository.save(comment);

        // Audit log
        auditService.logCreate("COMMENT", savedComment.getCommentId(),
                "Created comment on initiative: " + initiative.getTitle());

        // Notify mentioned users
        notifyMentionedUsers(mentionedUsers, currentUser, initiative, savedComment);

        // Notify other stakeholders (assigned users and managers) about new comment
        notifyStakeholders(initiative, currentUser, savedComment);

        return savedComment;
    }

    /**
     * Get all comments for an initiative
     */
    public List<Comment> getCommentsByInitiative(Long initiativeId) {
        // Verify initiative exists
        initiativeRepository.findById(initiativeId)
                .orElseThrow(() -> new RuntimeException("Initiative not found"));

        return commentRepository.findByInitiativeIdOrderByCreatedAtDesc(initiativeId);
    }

    /**
     * Update a comment
     */
    public Comment updateComment(Long commentId, Comment updatedComment) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = auth.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + currentUserEmail));

        // Authorization: Only comment author can update
        if (!comment.getAuthor().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("You can only update your own comments");
        }

        // Update content
        comment.setContent(updatedComment.getContent());

        // Process new mentions
        Set<User> newMentionedUsers = processMentions(updatedComment.getContent(), comment.getInitiative(), currentUser);
        Set<User> oldMentionedUsers = comment.getMentionedUsers();

        comment.setMentionedUsers(newMentionedUsers);

        Comment savedComment = commentRepository.save(comment);

        // Audit log
        auditService.logUpdate("COMMENT", commentId, "Updated comment on initiative: " + comment.getInitiative().getTitle());

        // Notify newly mentioned users (users in new set but not in old set)
        Set<Long> oldMentionedIds = oldMentionedUsers.stream()
                .map(User::getUserId)
                .collect(Collectors.toSet());

        Set<User> newlyMentioned = newMentionedUsers.stream()
                .filter(user -> !oldMentionedIds.contains(user.getUserId()))
                .collect(Collectors.toSet());

        notifyMentionedUsers(newlyMentioned, currentUser, comment.getInitiative(), savedComment);

        return savedComment;
    }

    /**
     * Soft delete a comment
     */
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = auth.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + currentUserEmail));

        // Authorization: Comment author or manager/admin can delete
        boolean isAuthor = comment.getAuthor().getUserId().equals(currentUser.getUserId());
        boolean isManagerOrAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAuthor && !isManagerOrAdmin) {
            throw new RuntimeException("You don't have permission to delete this comment");
        }

        // Soft delete
        comment.setDeleted(true);
        commentRepository.save(comment);

        // Audit log
        auditService.logDelete("COMMENT", commentId, "Deleted comment on initiative: " + comment.getInitiative().getTitle());
    }

    /**
     * Check if user can comment on an initiative
     */
    private boolean canComment(Initiative initiative, User user) {
        // Managers and Admins can always comment
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isManagerOrAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_ADMIN"));

        if (isManagerOrAdmin) {
            return true;
        }

        // Check if user is assigned to the initiative
        if (initiative.getAssignedUsers() != null && !initiative.getAssignedUsers().isEmpty()) {
            return initiative.getAssignedUsers().stream()
                    .anyMatch(assignedUser -> assignedUser.getUserId().equals(user.getUserId()));
        }

        return false;
    }

    /**
     * Process @mentions in comment content
     */
    private Set<User> processMentions(String content, Initiative initiative, User commentAuthor) {
        Set<User> mentionedUsers = new HashSet<>();

        if (content == null || content.trim().isEmpty()) {
            return mentionedUsers;
        }

        // Find all @mentions in the content
        Matcher matcher = MENTION_PATTERN.matcher(content);
        Set<String> mentionedUsernames = new HashSet<>();

        while (matcher.find()) {
            String mention = matcher.group(1); // Extract username part
            mentionedUsernames.add(mention.toLowerCase());
        }

        // Find users by username (from email) or name
        for (String username : mentionedUsernames) {
            // Try to find user by email username (part before @)
            List<User> users = userRepository.findAll().stream()
                    .filter(user -> {
                        if (user.getEmail() != null) {
                            String emailUsername = user.getEmail().split("@")[0].toLowerCase();
                            if (emailUsername.equals(username)) {
                                return true;
                            }
                        }
                        // Also check name (case-insensitive partial match)
                        if (user.getName() != null) {
                            String nameLower = user.getName().toLowerCase();
                            return nameLower.contains(username) || username.contains(nameLower);
                        }
                        return false;
                    })
                    .collect(Collectors.toList());

            // If multiple matches, prefer exact email username match
            User matchedUser = null;
            for (User user : users) {
                if (user.getEmail() != null) {
                    String emailUsername = user.getEmail().split("@")[0].toLowerCase();
                    if (emailUsername.equals(username)) {
                        matchedUser = user;
                        break;
                    }
                }
            }

            // If no exact match, use first match
            if (matchedUser == null && !users.isEmpty()) {
                matchedUser = users.get(0);
            }

            // Add user if found, active, and not the comment author
            // Managers and Admins can always be mentioned, even if not assigned to the initiative
            // Employees can be mentioned if they're assigned to the initiative OR if they're managers/admins
            if (matchedUser != null &&
                    "ACTIVE".equalsIgnoreCase(matchedUser.getStatus()) &&
                    !matchedUser.getUserId().equals(commentAuthor.getUserId())) {

                // Check if user can be mentioned
                boolean canBeMentioned = false;

                // Managers and Admins can always be mentioned
                String userRole = matchedUser.getRole();
                if ("MANAGER".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole)) {
                    canBeMentioned = true;
                } else {
                    // For employees, check if they're assigned to the initiative
                    canBeMentioned = canComment(initiative, matchedUser);
                }

                if (canBeMentioned) {
                    mentionedUsers.add(matchedUser);
                }
            }
        }

        return mentionedUsers;
    }

    /**
     * Notify mentioned users
     */
    private void notifyMentionedUsers(Set<User> mentionedUsers, User commentAuthor, Initiative initiative, Comment comment) {
        for (User mentionedUser : mentionedUsers) {
            try {
                notificationService.createNotification(
                        new CreateNotificationRequest(
                                mentionedUser.getUserId(),
                                "MENTION",
                                commentAuthor.getName() + " mentioned you in a comment on initiative: " + initiative.getTitle(),
                                "INITIATIVE",
                                initiative.getInitiativeId()
                        )
                );
                logger.info("Successfully sent mention notification: userId={}, email={}, initiativeId={}",
                        mentionedUser.getUserId(), mentionedUser.getEmail(), initiative.getInitiativeId());
            } catch (Exception e) {
                logger.error("Failed to send mention notification: userId={}, email={}, initiativeId={}",
                        mentionedUser.getUserId(), mentionedUser.getEmail(), initiative.getInitiativeId(), e);
            }
        }
    }

    /**
     * Notify stakeholders (assigned users and managers) about new comment
     */
    private void notifyStakeholders(Initiative initiative, User commentAuthor, Comment comment) {
        Set<User> stakeholders = new HashSet<>();

        // Add assigned users
        if (initiative.getAssignedUsers() != null) {
            stakeholders.addAll(initiative.getAssignedUsers());
        }

        // Add plan owner (manager)
        if (initiative.getMilestone() != null &&
                initiative.getMilestone().getPlan() != null &&
                initiative.getMilestone().getPlan().getUser() != null) {
            stakeholders.add(initiative.getMilestone().getPlan().getUser());
        }

        // Remove comment author and already mentioned users
        stakeholders.remove(commentAuthor);
        stakeholders.removeAll(comment.getMentionedUsers());

        // Notify stakeholders
        for (User stakeholder : stakeholders) {
            try {
                notificationService.createNotification(
                        new CreateNotificationRequest(
                                stakeholder.getUserId(),
                                "COMMENT",
                                commentAuthor.getName() + " commented on initiative: " + initiative.getTitle(),
                                "INITIATIVE",
                                initiative.getInitiativeId()
                        )
                );
            } catch (Exception e) {
                logger.error("Failed to notify stakeholder: userId={}, initiativeId={}",
                        stakeholder.getUserId(), initiative.getInitiativeId(), e);
            }
        }
    }
}
