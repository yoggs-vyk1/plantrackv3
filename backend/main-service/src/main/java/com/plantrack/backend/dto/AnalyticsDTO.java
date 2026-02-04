package com.plantrack.backend.dto;

public class AnalyticsDTO {
    private int totalPlans;
    private int completedPlans;
    private int pendingPlans;
    private double completionPercentage;

    public AnalyticsDTO(int totalPlans, int completedPlans, int pendingPlans, double completionPercentage) {
        this.totalPlans = totalPlans;
        this.completedPlans = completedPlans;
        this.pendingPlans = pendingPlans;
        this.completionPercentage = completionPercentage;
    }

    // Getters only (Setters not needed for a report)
    public int getTotalPlans() { return totalPlans; }
    public int getCompletedPlans() { return completedPlans; }
    public int getPendingPlans() { return pendingPlans; }
    public double getCompletionPercentage() { return completionPercentage; }
}