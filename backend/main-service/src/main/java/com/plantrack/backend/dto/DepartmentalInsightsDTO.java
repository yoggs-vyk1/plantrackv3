package com.plantrack.backend.dto;

import java.util.Map;

public class DepartmentalInsightsDTO {
    private String department;
    private int totalInitiatives;
    private int completedInitiatives;
    private int inProgressInitiatives;
    private int plannedInitiatives;
    private double completionRate;
    private double onTimeDeliveryRate;
    private int blockedCount;
    private Map<String, Integer> statusBreakdown;

    public DepartmentalInsightsDTO() {}

    public DepartmentalInsightsDTO(String department, int totalInitiatives, int completedInitiatives,
                                   int inProgressInitiatives, int plannedInitiatives, double completionRate,
                                   double onTimeDeliveryRate, int blockedCount, Map<String, Integer> statusBreakdown) {
        this.department = department;
        this.totalInitiatives = totalInitiatives;
        this.completedInitiatives = completedInitiatives;
        this.inProgressInitiatives = inProgressInitiatives;
        this.plannedInitiatives = plannedInitiatives;
        this.completionRate = completionRate;
        this.onTimeDeliveryRate = onTimeDeliveryRate;
        this.blockedCount = blockedCount;
        this.statusBreakdown = statusBreakdown;
    }

    // Getters and Setters
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getTotalInitiatives() { return totalInitiatives; }
    public void setTotalInitiatives(int totalInitiatives) { this.totalInitiatives = totalInitiatives; }

    public int getCompletedInitiatives() { return completedInitiatives; }
    public void setCompletedInitiatives(int completedInitiatives) { this.completedInitiatives = completedInitiatives; }

    public int getInProgressInitiatives() { return inProgressInitiatives; }
    public void setInProgressInitiatives(int inProgressInitiatives) { this.inProgressInitiatives = inProgressInitiatives; }

    public int getPlannedInitiatives() { return plannedInitiatives; }
    public void setPlannedInitiatives(int plannedInitiatives) { this.plannedInitiatives = plannedInitiatives; }

    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }

    public double getOnTimeDeliveryRate() { return onTimeDeliveryRate; }
    public void setOnTimeDeliveryRate(double onTimeDeliveryRate) { this.onTimeDeliveryRate = onTimeDeliveryRate; }

    public int getBlockedCount() { return blockedCount; }
    public void setBlockedCount(int blockedCount) { this.blockedCount = blockedCount; }

    public Map<String, Integer> getStatusBreakdown() { return statusBreakdown; }
    public void setStatusBreakdown(Map<String, Integer> statusBreakdown) { this.statusBreakdown = statusBreakdown; }
}

