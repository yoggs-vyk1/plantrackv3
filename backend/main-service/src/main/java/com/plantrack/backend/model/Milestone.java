package com.plantrack.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.plantrack.backend.config.LocalDateTimeDeserializer;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "milestones")
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long milestoneId;

    @Column(nullable = false)
    @NotBlank(message = "Title is required")
    private String title;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime dueDate;

    // --- NEW FIELDS NEEDED FOR AUTOMATION ---
    private Double completionPercent;
    
    private String status; // "PLANNED", "IN_PROGRESS", "COMPLETED"

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonIgnoreProperties({"milestones", "user"}) // Prevent circular reference but allow plan data
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // Allow serialization, prevent deserialization (plan is set via service, not request body)
    private Plan plan;

    @OneToMany(mappedBy = "milestone", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private java.util.List<Initiative> initiatives = new java.util.ArrayList<>();

    public Milestone() {
        // Default values
        this.completionPercent = 0.0;
        this.status = "PLANNED";
    }

    // --- Getters and Setters ---
    public Long getMilestoneId() { return milestoneId; }
    public void setMilestoneId(Long milestoneId) { this.milestoneId = milestoneId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public Double getCompletionPercent() { return completionPercent; }
    public void setCompletionPercent(Double completionPercent) { this.completionPercent = completionPercent; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }

    public java.util.List<Initiative> getInitiatives() { return initiatives; }
    public void setInitiatives(java.util.List<Initiative> initiatives) { this.initiatives = initiatives; }
}