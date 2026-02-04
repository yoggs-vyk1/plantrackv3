package com.plantrack.backend.dto;

import java.util.List;

public class GamifiedVelocityDTO {
    private Long userId;
    private String userName;
    private String department;
    private int tasksAssigned;
    private int tasksCompleted;
    private double completionRate;
    private double averageTasksPerWeek;
    private double averageTasksPerMonth;
    private double overallScore;
    private int rank;
    private int departmentRank;
    private String performanceTier;
    private List<BadgeDTO> badges;
    private double improvementPercentage;
    private int streakDays;
    private int streakWeeks;

    public GamifiedVelocityDTO() {}

    public GamifiedVelocityDTO(Long userId, String userName, String department,
                               int tasksAssigned, int tasksCompleted, double completionRate,
                               double averageTasksPerWeek, double averageTasksPerMonth,
                               double overallScore, int rank, int departmentRank,
                               String performanceTier, List<BadgeDTO> badges,
                               double improvementPercentage, int streakDays, int streakWeeks) {
        this.userId = userId;
        this.userName = userName;
        this.department = department;
        this.tasksAssigned = tasksAssigned;
        this.tasksCompleted = tasksCompleted;
        this.completionRate = completionRate;
        this.averageTasksPerWeek = averageTasksPerWeek;
        this.averageTasksPerMonth = averageTasksPerMonth;
        this.overallScore = overallScore;
        this.rank = rank;
        this.departmentRank = departmentRank;
        this.performanceTier = performanceTier;
        this.badges = badges;
        this.improvementPercentage = improvementPercentage;
        this.streakDays = streakDays;
        this.streakWeeks = streakWeeks;
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getTasksAssigned() { return tasksAssigned; }
    public void setTasksAssigned(int tasksAssigned) { this.tasksAssigned = tasksAssigned; }

    public int getTasksCompleted() { return tasksCompleted; }
    public void setTasksCompleted(int tasksCompleted) { this.tasksCompleted = tasksCompleted; }

    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }

    public double getAverageTasksPerWeek() { return averageTasksPerWeek; }
    public void setAverageTasksPerWeek(double averageTasksPerWeek) { this.averageTasksPerWeek = averageTasksPerWeek; }

    public double getAverageTasksPerMonth() { return averageTasksPerMonth; }
    public void setAverageTasksPerMonth(double averageTasksPerMonth) { this.averageTasksPerMonth = averageTasksPerMonth; }

    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public int getDepartmentRank() { return departmentRank; }
    public void setDepartmentRank(int departmentRank) { this.departmentRank = departmentRank; }

    public String getPerformanceTier() { return performanceTier; }
    public void setPerformanceTier(String performanceTier) { this.performanceTier = performanceTier; }

    public List<BadgeDTO> getBadges() { return badges; }
    public void setBadges(List<BadgeDTO> badges) { this.badges = badges; }

    public double getImprovementPercentage() { return improvementPercentage; }
    public void setImprovementPercentage(double improvementPercentage) { this.improvementPercentage = improvementPercentage; }

    public int getStreakDays() { return streakDays; }
    public void setStreakDays(int streakDays) { this.streakDays = streakDays; }

    public int getStreakWeeks() { return streakWeeks; }
    public void setStreakWeeks(int streakWeeks) { this.streakWeeks = streakWeeks; }
}

