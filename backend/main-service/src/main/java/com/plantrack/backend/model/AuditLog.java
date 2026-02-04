package com.plantrack.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;       // e.g., "CREATE", "UPDATE", "DELETE", "UPDATE_STATUS"

    @Column(nullable = false)
    private String performedBy;  // e.g., "bob@corp.com" or user ID

    @Column(nullable = false)
    private String entityType;   // e.g., "PLAN", "MILESTONE", "INITIATIVE", "USER"

    private Long entityId;       // ID of the entity that was modified

    @Column(length = 1000)
    private String details;      // e.g., "Changed Status from PLANNED to COMPLETED"

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // Additional context fields
    private String oldValue;      // Previous value (for updates)
    private String newValue;      // New value (for updates)

    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }

    public AuditLog(String action, String performedBy, String entityType, Long entityId, String details) {
        this.action = action;
        this.performedBy = performedBy;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
}