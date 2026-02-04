package com.plantrack.backend.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class ReportMetrics {

    private double avgGoalCompletion;      // Average % of high-level Plans
    private double avgMilestoneCompletion; // Average % of Milestones
    private double avgInitiativeCompletion; // Average % of Initiatives (Tasks)
    private int totalUsers;

    public ReportMetrics() {}

    public ReportMetrics(double avgGoalCompletion, double avgMilestoneCompletion, double avgInitiativeCompletion, int totalUsers) {
        this.avgGoalCompletion = avgGoalCompletion;
        this.avgMilestoneCompletion = avgMilestoneCompletion;
        this.avgInitiativeCompletion = avgInitiativeCompletion;
        this.totalUsers = totalUsers;
    }

    // Getters only (Reports are usually read-only snapshots)
    public double getAvgGoalCompletion() { return avgGoalCompletion; }
    public double getAvgMilestoneCompletion() { return avgMilestoneCompletion; }
    public double getAvgInitiativeCompletion() { return avgInitiativeCompletion; }
    public int getTotalUsers() { return totalUsers; }
}