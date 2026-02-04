package com.plantrack.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "initiatives")
public class Initiative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long initiativeId;

    @Column(nullable = false)
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @Column(nullable = false)
    private String status; // PLANNED, IN_PROGRESS, COMPLETED

    // --- RELATIONSHIP: Initiative belongs to a Milestone ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "milestone_id", nullable = false)
    @JsonIgnoreProperties({"initiatives", "plan"}) // Prevent circular reference but allow milestone data
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // Allow serialization, prevent deserialization (milestone is set via service, not request body)
    private Milestone milestone;

    // --- RELATIONSHIP: Initiative is assigned to multiple Users (Employees) ---
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "initiative_assignees",
        joinColumns = @JoinColumn(name = "initiative_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties({"password", "plans"}) // Prevent password and circular references
    private Set<User> assignedUsers = new HashSet<>();

    // --- RELATIONSHIP: Initiative has many Comments ---
    @OneToMany(mappedBy = "initiative", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL)
    @JsonIgnoreProperties({"initiative", "mentionedUsers"}) // Prevent circular references
    private List<Comment> comments = new ArrayList<>();

    public Initiative() {}

    // Getters and Setters
    public Long getInitiativeId() { return initiativeId; }
    public void setInitiativeId(Long initiativeId) { this.initiativeId = initiativeId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Milestone getMilestone() { return milestone; }
    public void setMilestone(Milestone milestone) { this.milestone = milestone; }

    public Set<User> getAssignedUsers() { return assignedUsers; }
    public void setAssignedUsers(Set<User> assignedUsers) { this.assignedUsers = assignedUsers; }

    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
}