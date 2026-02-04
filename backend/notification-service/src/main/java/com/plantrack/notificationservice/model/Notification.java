package com.plantrack.notificationservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
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

    @Column(name = "user_id", nullable = false)
    private Long user;

    public Notification() {
        this.createdDate = LocalDateTime.now();
        this.status = "UNREAD";
    }

}