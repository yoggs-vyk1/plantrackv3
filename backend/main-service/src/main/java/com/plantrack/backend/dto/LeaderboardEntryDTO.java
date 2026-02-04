package com.plantrack.backend.dto;

public class LeaderboardEntryDTO {
    private Long userId;
    private String userName;
    private String department;
    private int rank;
    private double score;
    private String metricType; // OVERALL, SPEED, QUALITY, IMPROVEMENT
    private double metricValue;
    private int rankChange; // Positive = moved up, Negative = moved down, 0 = no change
    private String badgeIcon; // Optional badge icon

    public LeaderboardEntryDTO() {}

    public LeaderboardEntryDTO(Long userId, String userName, String department, int rank,
                               double score, String metricType, double metricValue,
                               int rankChange, String badgeIcon) {
        this.userId = userId;
        this.userName = userName;
        this.department = department;
        this.rank = rank;
        this.score = score;
        this.metricType = metricType;
        this.metricValue = metricValue;
        this.rankChange = rankChange;
        this.badgeIcon = badgeIcon;
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }

    public double getMetricValue() { return metricValue; }
    public void setMetricValue(double metricValue) { this.metricValue = metricValue; }

    public int getRankChange() { return rankChange; }
    public void setRankChange(int rankChange) { this.rankChange = rankChange; }

    public String getBadgeIcon() { return badgeIcon; }
    public void setBadgeIcon(String badgeIcon) { this.badgeIcon = badgeIcon; }
}

