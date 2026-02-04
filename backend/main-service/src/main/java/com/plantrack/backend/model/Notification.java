package com.plantrack.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @Column(nullable = false)
    private String type; // e.g., "ASSIGNMENT", "STATUS_UPDATE", "SYSTEM_ALERT", "WEEKLY_REPORT"

    @Column(nullable = false)
    private String message;

    private String status; // "UNREAD", "READ"

    private LocalDateTime createdDate;

    // Link to related entity (Plan, Initiative, etc.)
    private String entityType; // "PLAN", "INITIATIVE", "MILESTONE", "SYSTEM"
    private Long entityId; // ID of the related entity

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Notification() {
        this.createdDate = LocalDateTime.now();
        this.status = "UNREAD";
    }

    // Getters and Setters
    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}