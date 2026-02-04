package com.plantrack.backend.dto;

import java.time.LocalDate;
import java.util.Map;

public class VelocityMetricsDTO {
    private Long userId;
    private String userName;
    private String department;
    private int tasksAssigned;
    private int tasksCompleted;
    private double completionRate;
    private Map<LocalDate, Integer> weeklyVelocity; // Date -> tasks completed
    private Map<String, Integer> monthlyVelocity; // Month -> tasks completed
    private double averageTasksPerWeek;
    private double averageTasksPerMonth;

    public VelocityMetricsDTO() {}

    public VelocityMetricsDTO(Long userId, String userName, String department, int tasksAssigned,
                             int tasksCompleted, double completionRate, Map<LocalDate, Integer> weeklyVelocity,
                             Map<String, Integer> monthlyVelocity, double averageTasksPerWeek, double averageTasksPerMonth) {
        this.userId = userId;
        this.userName = userName;
        this.department = department;
        this.tasksAssigned = tasksAssigned;
        this.tasksCompleted = tasksCompleted;
        this.completionRate = completionRate;
        this.weeklyVelocity = weeklyVelocity;
        this.monthlyVelocity = monthlyVelocity;
        this.averageTasksPerWeek = averageTasksPerWeek;
        this.averageTasksPerMonth = averageTasksPerMonth;
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

    public Map<LocalDate, Integer> getWeeklyVelocity() { return weeklyVelocity; }
    public void setWeeklyVelocity(Map<LocalDate, Integer> weeklyVelocity) { this.weeklyVelocity = weeklyVelocity; }

    public Map<String, Integer> getMonthlyVelocity() { return monthlyVelocity; }
    public void setMonthlyVelocity(Map<String, Integer> monthlyVelocity) { this.monthlyVelocity = monthlyVelocity; }

    public double getAverageTasksPerWeek() { return averageTasksPerWeek; }
    public void setAverageTasksPerWeek(double averageTasksPerWeek) { this.averageTasksPerWeek = averageTasksPerWeek; }

    public double getAverageTasksPerMonth() { return averageTasksPerMonth; }
    public void setAverageTasksPerMonth(double averageTasksPerMonth) { this.averageTasksPerMonth = averageTasksPerMonth; }
}

