package com.plantrack.backend.dto;

public class DashboardStatsDTO {
    private int totalPlans;
    private int activeInitiatives;
    private int completedMilestones;
    private int totalUsers;

    public DashboardStatsDTO() {}

    public DashboardStatsDTO(int totalPlans, int activeInitiatives, int completedMilestones, int totalUsers) {
        this.totalPlans = totalPlans;
        this.activeInitiatives = activeInitiatives;
        this.completedMilestones = completedMilestones;
        this.totalUsers = totalUsers;
    }

    public int getTotalPlans() {
        return totalPlans;
    }

    public void setTotalPlans(int totalPlans) {
        this.totalPlans = totalPlans;
    }

    public int getActiveInitiatives() {
        return activeInitiatives;
    }

    public void setActiveInitiatives(int activeInitiatives) {
        this.activeInitiatives = activeInitiatives;
    }

    public int getCompletedMilestones() {
        return completedMilestones;
    }

    public void setCompletedMilestones(int completedMilestones) {
        this.completedMilestones = completedMilestones;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }
}

