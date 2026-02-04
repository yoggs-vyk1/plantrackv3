package com.plantrack.backend.dto;

import java.time.LocalDateTime;

public class BadgeDTO {
    private String badgeId;
    private String badgeName;
    private String description;
    private String category; // SPEED, QUALITY, CONSISTENCY, TEAMWORK, IMPROVEMENT
    private String icon; // Icon identifier
    private LocalDateTime earnedDate;
    private boolean earned;
    private String criteria;

    public BadgeDTO() {}

    public BadgeDTO(String badgeId, String badgeName, String description, String category,
                   String icon, LocalDateTime earnedDate, boolean earned, String criteria) {
        this.badgeId = badgeId;
        this.badgeName = badgeName;
        this.description = description;
        this.category = category;
        this.icon = icon;
        this.earnedDate = earnedDate;
        this.earned = earned;
        this.criteria = criteria;
    }

    // Getters and Setters
    public String getBadgeId() { return badgeId; }
    public void setBadgeId(String badgeId) { this.badgeId = badgeId; }

    public String getBadgeName() { return badgeName; }
    public void setBadgeName(String badgeName) { this.badgeName = badgeName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public LocalDateTime getEarnedDate() { return earnedDate; }
    public void setEarnedDate(LocalDateTime earnedDate) { this.earnedDate = earnedDate; }

    public boolean isEarned() { return earned; }
    public void setEarned(boolean earned) { this.earned = earned; }

    public String getCriteria() { return criteria; }
    public void setCriteria(String criteria) { this.criteria = criteria; }
}

