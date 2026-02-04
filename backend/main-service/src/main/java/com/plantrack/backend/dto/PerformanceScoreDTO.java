package com.plantrack.backend.dto;

public class PerformanceScoreDTO {
    private Long userId;
    private String userName;
    private String department;
    private double overallScore;
    private double completionRate;
    private double speedScore;
    private double qualityScore;
    private double consistencyScore;
    private int rank;
    private int departmentRank;
    private int previousRank;
    private int previousDepartmentRank;
    private String performanceTier; // TOP_PERFORMER, CONSISTENT, NEEDS_IMPROVEMENT
    private double improvementPercentage;

    public PerformanceScoreDTO() {}

    public PerformanceScoreDTO(Long userId, String userName, String department, double overallScore,
                               double completionRate, double speedScore, double qualityScore,
                               double consistencyScore, int rank, int departmentRank,
                               int previousRank, int previousDepartmentRank, String performanceTier,
                               double improvementPercentage) {
        this.userId = userId;
        this.userName = userName;
        this.department = department;
        this.overallScore = overallScore;
        this.completionRate = completionRate;
        this.speedScore = speedScore;
        this.qualityScore = qualityScore;
        this.consistencyScore = consistencyScore;
        this.rank = rank;
        this.departmentRank = departmentRank;
        this.previousRank = previousRank;
        this.previousDepartmentRank = previousDepartmentRank;
        this.performanceTier = performanceTier;
        this.improvementPercentage = improvementPercentage;
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }

    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }

    public double getSpeedScore() { return speedScore; }
    public void setSpeedScore(double speedScore) { this.speedScore = speedScore; }

    public double getQualityScore() { return qualityScore; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }

    public double getConsistencyScore() { return consistencyScore; }
    public void setConsistencyScore(double consistencyScore) { this.consistencyScore = consistencyScore; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public int getDepartmentRank() { return departmentRank; }
    public void setDepartmentRank(int departmentRank) { this.departmentRank = departmentRank; }

    public int getPreviousRank() { return previousRank; }
    public void setPreviousRank(int previousRank) { this.previousRank = previousRank; }

    public int getPreviousDepartmentRank() { return previousDepartmentRank; }
    public void setPreviousDepartmentRank(int previousDepartmentRank) { this.previousDepartmentRank = previousDepartmentRank; }

    public String getPerformanceTier() { return performanceTier; }
    public void setPerformanceTier(String performanceTier) { this.performanceTier = performanceTier; }

    public double getImprovementPercentage() { return improvementPercentage; }
    public void setImprovementPercentage(double improvementPercentage) { this.improvementPercentage = improvementPercentage; }
}

