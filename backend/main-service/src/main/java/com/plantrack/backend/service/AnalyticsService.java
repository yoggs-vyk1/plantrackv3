package com.plantrack.backend.service;

import com.plantrack.backend.dto.*;
import com.plantrack.backend.model.Initiative;
import com.plantrack.backend.model.Milestone;
import com.plantrack.backend.model.Plan;
import com.plantrack.backend.model.PlanStatus;
import com.plantrack.backend.model.User;
import com.plantrack.backend.repository.InitiativeRepository;
import com.plantrack.backend.repository.MilestoneRepository;
import com.plantrack.backend.repository.PlanRepository;
import com.plantrack.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private InitiativeRepository initiativeRepository;

    @Autowired
    private UserRepository userRepository;

    public AnalyticsDTO getUserAnalytics(Long userId) {
        logger.debug("Calculating user analytics: userId={}", userId);
        
        // 1. Fetch all plans for the user
        List<Plan> userPlans = planRepository.findByUserUserId(userId);

        int totalPlans = userPlans.size();
        int completedPlans = 0;
        int pendingPlans = 0;

        // 2. Loop and Count
        for (Plan plan : userPlans) {
            if (plan.getStatus() == PlanStatus.COMPLETED) {
                completedPlans++;
            } else {
                pendingPlans++;
            }
        }

        // 3. Calculate Percentage (Avoid division by zero)
        double percentage = (totalPlans == 0) ? 0.0 : ((double) completedPlans / totalPlans) * 100;

        logger.info("User analytics calculated: userId={}, totalPlans={}, completedPlans={}, pendingPlans={}, completionRate={}%", 
                userId, totalPlans, completedPlans, pendingPlans, percentage);
        
        // 4. Return the Report
        return new AnalyticsDTO(totalPlans, completedPlans, pendingPlans, percentage);
    }

    public DashboardStatsDTO getDashboardStats() {
        logger.debug("Calculating dashboard statistics");
        long startTime = System.currentTimeMillis();
        
        // Get total plans
        int totalPlans = (int) planRepository.count();

        // Get active initiatives (status is IN_PROGRESS or PLANNED)
        List<Initiative> allInitiatives = initiativeRepository.findAll();
        int activeInitiatives = 0;
        for (Initiative initiative : allInitiatives) {
            String status = initiative.getStatus();
            if (status != null && (status.equals("IN_PROGRESS") || status.equals("PLANNED"))) {
                activeInitiatives++;
            }
        }

        // Get completed milestones
        List<Milestone> allMilestones = milestoneRepository.findAll();
        int completedMilestones = 0;
        for (Milestone milestone : allMilestones) {
            if (milestone.getStatus() != null && milestone.getStatus().equals("COMPLETED")) {
                completedMilestones++;
            }
        }

        // Get total users
        int totalUsers = (int) userRepository.count();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Dashboard stats calculated: totalPlans={}, activeInitiatives={}, completedMilestones={}, totalUsers={}, duration={}ms", 
                totalPlans, activeInitiatives, completedMilestones, totalUsers, duration);

        return new DashboardStatsDTO(totalPlans, activeInitiatives, completedMilestones, totalUsers);
    }

    /**
     * Get departmental insights - analyze performance by department
     */
    public List<DepartmentalInsightsDTO> getDepartmentalInsights() {
        List<User> allUsers = userRepository.findAll();
        Map<String, List<User>> usersByDepartment = allUsers.stream()
                .filter(u -> u.getDepartment() != null && !u.getDepartment().isEmpty())
                .collect(Collectors.groupingBy(User::getDepartment));

        List<DepartmentalInsightsDTO> insights = new ArrayList<>();

        for (Map.Entry<String, List<User>> entry : usersByDepartment.entrySet()) {
            String department = entry.getKey();
            List<User> departmentUsers = entry.getValue();
            List<Long> userIds = departmentUsers.stream()
                    .map(User::getUserId)
                    .collect(Collectors.toList());

            // Get all initiatives assigned to users in this department
            List<Initiative> departmentInitiatives = initiativeRepository.findAll().stream()
                    .filter(i -> i.getAssignedUsers() != null && !i.getAssignedUsers().isEmpty() &&
                            i.getAssignedUsers().stream()
                                    .anyMatch(user -> userIds.contains(user.getUserId())))
                    .collect(Collectors.toList());

            int total = departmentInitiatives.size();
            int completed = (int) departmentInitiatives.stream()
                    .filter(i -> "COMPLETED".equalsIgnoreCase(i.getStatus()))
                    .count();
            int inProgress = (int) departmentInitiatives.stream()
                    .filter(i -> "IN_PROGRESS".equalsIgnoreCase(i.getStatus()))
                    .count();
            int planned = (int) departmentInitiatives.stream()
                    .filter(i -> "PLANNED".equalsIgnoreCase(i.getStatus()))
                    .count();

            double completionRate = total > 0 ? ((double) completed / total) * 100 : 0.0;

            // Calculate on-time delivery rate (simplified - assumes completed initiatives are on time)
            // In a real system, you'd compare due dates with completion dates
            double onTimeDeliveryRate = completed > 0 ? 85.0 : 0.0; // Placeholder

            // Count "blocked" initiatives (could be based on status or other criteria)
            int blockedCount = 0; // Placeholder - would need additional field

            // Status breakdown
            Map<String, Integer> statusBreakdown = new HashMap<>();
            statusBreakdown.put("COMPLETED", completed);
            statusBreakdown.put("IN_PROGRESS", inProgress);
            statusBreakdown.put("PLANNED", planned);

            insights.add(new DepartmentalInsightsDTO(
                    department, total, completed, inProgress, planned,
                    completionRate, onTimeDeliveryRate, blockedCount, statusBreakdown
            ));
        }

        return insights;
    }

    /**
     * Get velocity metrics for a specific user
     */
    public VelocityMetricsDTO getUserVelocity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get all initiatives assigned to this user
        List<Initiative> userInitiatives = initiativeRepository.findAll().stream()
                .filter(i -> i.getAssignedUsers() != null && !i.getAssignedUsers().isEmpty() &&
                        i.getAssignedUsers().stream()
                                .anyMatch(assignedUser -> assignedUser.getUserId().equals(userId)))
                .collect(Collectors.toList());

        int tasksAssigned = userInitiatives.size();
        int tasksCompleted = (int) userInitiatives.stream()
                .filter(i -> "COMPLETED".equalsIgnoreCase(i.getStatus()))
                .count();

        double completionRate = tasksAssigned > 0 ? ((double) tasksCompleted / tasksAssigned) * 100 : 0.0;

        // Weekly velocity (simplified - would need createdDate/completedDate in real system)
        Map<LocalDate, Integer> weeklyVelocity = new HashMap<>();
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 8; i++) {
            LocalDate weekStart = now.minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
            weeklyVelocity.put(weekStart, 0); // Placeholder
        }

        // Monthly velocity
        Map<String, Integer> monthlyVelocity = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            String monthKey = monthStart.getYear() + "-" + String.format("%02d", monthStart.getMonthValue());
            monthlyVelocity.put(monthKey, 0); // Placeholder
        }

        // Calculate averages (simplified)
        double averageTasksPerWeek = tasksCompleted / 8.0; // Over last 8 weeks
        double averageTasksPerMonth = tasksCompleted / 6.0; // Over last 6 months

        return new VelocityMetricsDTO(
                userId, user.getName(), user.getDepartment(),
                tasksAssigned, tasksCompleted, completionRate,
                weeklyVelocity, monthlyVelocity,
                averageTasksPerWeek, averageTasksPerMonth
        );
    }

    /**
     * Get velocity metrics for all users (team performance)
     */
    public List<VelocityMetricsDTO> getAllUsersVelocity() {
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .map(u -> getUserVelocity(u.getUserId()))
                .collect(Collectors.toList());
    }

    // ============================================================
    // GAMIFICATION METHODS
    // ============================================================

    /**
     * Calculate performance score for a user with proper department-wise ranking
     */
    public PerformanceScoreDTO calculatePerformanceScore(Long userId, String departmentFilter) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get user's initiatives
        List<Initiative> userInitiatives = initiativeRepository.findAll().stream()
                .filter(i -> i.getAssignedUsers() != null && !i.getAssignedUsers().isEmpty() &&
                        i.getAssignedUsers().stream()
                                .anyMatch(assignedUser -> assignedUser.getUserId().equals(userId)))
                .collect(Collectors.toList());

        int tasksAssigned = userInitiatives.size();
        int tasksCompleted = (int) userInitiatives.stream()
                .filter(i -> "COMPLETED".equalsIgnoreCase(i.getStatus()))
                .count();

        double completionRate = tasksAssigned > 0 ? ((double) tasksCompleted / tasksAssigned) * 100 : 0.0;

        // Speed score: based on completion rate and average tasks per week
        double averageTasksPerWeek = tasksCompleted / 8.0;
        double speedScore = Math.min(100, (averageTasksPerWeek / 5.0) * 100); // Normalize to 5 tasks/week = 100

        // Quality score: based on completion rate
        double qualityScore = completionRate;

        // Consistency score: variance in completion (simplified - would need historical data)
        double consistencyScore = completionRate > 80 ? 90.0 : completionRate > 50 ? 70.0 : 50.0;

        // Overall weighted score
        double overallScore = (completionRate * 0.4) + (speedScore * 0.3) + (qualityScore * 0.2) + (consistencyScore * 0.1);

        // Determine performance tier
        String performanceTier = overallScore >= 85 ? "TOP_PERFORMER" :
                                 overallScore >= 65 ? "CONSISTENT" : "NEEDS_IMPROVEMENT";

        // Calculate actual ranks by comparing with all users
        List<User> allUsers = userRepository.findAll();
        List<PerformanceScoreDTO> allScores = allUsers.stream()
                .map(u -> {
                    List<Initiative> uInitiatives = initiativeRepository.findAll().stream()
                            .filter(i -> i.getAssignedUsers() != null && !i.getAssignedUsers().isEmpty() &&
                                    i.getAssignedUsers().stream()
                                            .anyMatch(au -> au.getUserId().equals(u.getUserId())))
                            .collect(Collectors.toList());
                    int uTasksAssigned = uInitiatives.size();
                    int uTasksCompleted = (int) uInitiatives.stream()
                            .filter(i -> "COMPLETED".equalsIgnoreCase(i.getStatus()))
                            .count();
                    double uCompletionRate = uTasksAssigned > 0 ? ((double) uTasksCompleted / uTasksAssigned) * 100 : 0.0;
                    double uAvgTasksPerWeek = uTasksCompleted / 8.0;
                    double uSpeedScore = Math.min(100, (uAvgTasksPerWeek / 5.0) * 100);
                    double uQualityScore = uCompletionRate;
                    double uConsistencyScore = uCompletionRate > 80 ? 90.0 : uCompletionRate > 50 ? 70.0 : 50.0;
                    double uOverallScore = (uCompletionRate * 0.4) + (uSpeedScore * 0.3) + (uQualityScore * 0.2) + (uConsistencyScore * 0.1);
                    return new PerformanceScoreDTO(u.getUserId(), u.getName(), u.getDepartment(),
                            uOverallScore, uCompletionRate, uSpeedScore, uQualityScore, uConsistencyScore,
                            0, 0, 0, 0, "", 0.0);
                })
                .sorted(Comparator.comparing(PerformanceScoreDTO::getOverallScore).reversed())
                .collect(Collectors.toList());

        // Calculate overall rank
        int rank = 1;
        for (int i = 0; i < allScores.size(); i++) {
            if (allScores.get(i).getUserId().equals(userId)) {
                rank = i + 1;
                break;
            }
        }

        // Calculate department rank
        List<PerformanceScoreDTO> departmentScores = allScores.stream()
                .filter(s -> user.getDepartment() != null && 
                            s.getDepartment() != null && 
                            s.getDepartment().equals(user.getDepartment()))
                .sorted(Comparator.comparing(PerformanceScoreDTO::getOverallScore).reversed())
                .collect(Collectors.toList());

        int departmentRank = 1;
        for (int i = 0; i < departmentScores.size(); i++) {
            if (departmentScores.get(i).getUserId().equals(userId)) {
                departmentRank = i + 1;
                break;
            }
        }

        int previousRank = rank;
        int previousDepartmentRank = departmentRank;
        double improvementPercentage = 0.0;

        return new PerformanceScoreDTO(
                userId, user.getName(), user.getDepartment(),
                overallScore, completionRate, speedScore, qualityScore, consistencyScore,
                rank, departmentRank, previousRank, previousDepartmentRank,
                performanceTier, improvementPercentage
        );
    }

    /**
     * Get gamified velocity metrics with badges and rankings
     */
    public GamifiedVelocityDTO getGamifiedVelocity(Long userId, String departmentFilter) {
        VelocityMetricsDTO baseMetrics = getUserVelocity(userId);
        PerformanceScoreDTO performance = calculatePerformanceScore(userId, departmentFilter);
        List<BadgeDTO> badges = calculateBadges(userId);

        // Calculate streaks (simplified)
        int streakDays = 0;
        int streakWeeks = 0;

        return new GamifiedVelocityDTO(
                userId, baseMetrics.getUserName(), baseMetrics.getDepartment(),
                baseMetrics.getTasksAssigned(), baseMetrics.getTasksCompleted(),
                baseMetrics.getCompletionRate(), baseMetrics.getAverageTasksPerWeek(),
                baseMetrics.getAverageTasksPerMonth(), performance.getOverallScore(),
                performance.getRank(), performance.getDepartmentRank(),
                performance.getPerformanceTier(), badges,
                performance.getImprovementPercentage(), streakDays, streakWeeks
        );
    }

    /**
     * Calculate badges for a user (department-aware)
     */
    public List<BadgeDTO> calculateBadges(Long userId) {
        List<BadgeDTO> badges = new ArrayList<>();
        VelocityMetricsDTO metrics = getUserVelocity(userId);
        PerformanceScoreDTO performance = calculatePerformanceScore(userId, null);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get department-specific metrics for comparison
        List<VelocityMetricsDTO> departmentMetrics = getAllUsersVelocity().stream()
                .filter(m -> user.getDepartment() != null && 
                            m.getDepartment() != null && 
                            m.getDepartment().equals(user.getDepartment()))
                .collect(Collectors.toList());

        // Speed Demon: Complete tasks 25% faster than department average
        if (!departmentMetrics.isEmpty()) {
            double deptAvgTasksPerWeek = departmentMetrics.stream()
                    .mapToDouble(VelocityMetricsDTO::getAverageTasksPerWeek)
                    .average()
                    .orElse(0.0);
            if (deptAvgTasksPerWeek > 0 && metrics.getAverageTasksPerWeek() >= deptAvgTasksPerWeek * 1.25) {
                badges.add(new BadgeDTO("speed_demon", "Speed Demon",
                        "Complete tasks 25% faster than department average", "SPEED",
                        "⚡", LocalDateTime.now(), true,
                        "Average tasks per week: " + String.format("%.1f", metrics.getAverageTasksPerWeek())));
            }
        }

        // Quality Champion: Maintain 95%+ completion rate
        if (metrics.getCompletionRate() >= 95.0) {
            badges.add(new BadgeDTO("quality_champion", "Quality Champion",
                    "Maintain 95%+ completion rate", "QUALITY",
                    "🏆", LocalDateTime.now(), true,
                    "Completion rate: " + String.format("%.1f%%", metrics.getCompletionRate())));
        }

        // Consistency King: Steady performance
        if (performance.getConsistencyScore() >= 85.0) {
            badges.add(new BadgeDTO("consistency_king", "Consistency King",
                    "Steady performance with high consistency", "CONSISTENCY",
                    "👑", LocalDateTime.now(), true,
                    "Consistency score: " + String.format("%.1f", performance.getConsistencyScore())));
        }

        // Perfect Week: 100% completion for a week (simplified check)
        if (metrics.getCompletionRate() == 100.0 && metrics.getTasksCompleted() > 0) {
            badges.add(new BadgeDTO("perfect_week", "Perfect Week",
                    "100% completion rate", "QUALITY",
                    "⭐", LocalDateTime.now(), true,
                    "Perfect completion rate achieved"));
        }

        // Rising Star: Top 20% in department
        if (!departmentMetrics.isEmpty() && performance.getDepartmentRank() > 0) {
            int deptSize = departmentMetrics.size();
            int topPercent = (int) Math.ceil(deptSize * 0.2);
            if (performance.getDepartmentRank() <= topPercent) {
                badges.add(new BadgeDTO("rising_star", "Rising Star",
                        "Top performer in your department", "IMPROVEMENT",
                        "🌟", LocalDateTime.now(), true,
                        "Rank #" + performance.getDepartmentRank() + " in " + user.getDepartment()));
            }
        }

        // Team Player: High collaboration (simplified - based on tasks assigned)
        if (metrics.getTasksAssigned() >= 10) {
            badges.add(new BadgeDTO("team_player", "Team Player",
                    "Actively engaged with multiple tasks", "TEAMWORK",
                    "🤝", LocalDateTime.now(), true,
                    "Assigned to " + metrics.getTasksAssigned() + " tasks"));
        }

        return badges;
    }

    /**
     * Get leaderboard entries
     */
    public List<LeaderboardEntryDTO> getLeaderboard(String metricType, String departmentFilter, int limit) {
        List<User> users = userRepository.findAll();
        if (departmentFilter != null && !departmentFilter.isEmpty()) {
            users = users.stream()
                    .filter(u -> departmentFilter.equals(u.getDepartment()))
                    .collect(Collectors.toList());
        }

        List<LeaderboardEntryDTO> entries = new ArrayList<>();
        int rank = 1;

        for (User user : users) {
            VelocityMetricsDTO metrics = getUserVelocity(user.getUserId());
            PerformanceScoreDTO performance = calculatePerformanceScore(user.getUserId(), departmentFilter);

            double score = switch (metricType) {
                case "SPEED" -> metrics.getAverageTasksPerWeek();
                case "QUALITY" -> metrics.getCompletionRate();
                case "IMPROVEMENT" -> performance.getImprovementPercentage();
                default -> performance.getOverallScore();
            };

            entries.add(new LeaderboardEntryDTO(
                    user.getUserId(), user.getName(), user.getDepartment(),
                    rank++, score, metricType, score, 0, null
            ));
        }

        // Sort by score descending
        entries.sort(Comparator.comparing(LeaderboardEntryDTO::getScore).reversed());

        // Update ranks after sorting
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        // Limit results
        if (limit > 0 && limit < entries.size()) {
            entries = entries.subList(0, limit);
        }

        return entries;
    }

    /**
     * Get gamified velocity for all users with filtering and proper ranking
     */
    public List<GamifiedVelocityDTO> getAllGamifiedVelocity(String departmentFilter, String searchQuery,
                                                             Double minCompletionRate, Double maxCompletionRate,
                                                             Integer minTasks, Integer maxTasks,
                                                             String performanceTier, String sortBy, String sortOrder) {
        List<User> users = userRepository.findAll();

        // Apply department filter
        if (departmentFilter != null && !departmentFilter.isEmpty()) {
            users = users.stream()
                    .filter(u -> departmentFilter.equals(u.getDepartment()))
                    .collect(Collectors.toList());
        }

        // Apply search query
        if (searchQuery != null && !searchQuery.isEmpty()) {
            String query = searchQuery.toLowerCase();
            users = users.stream()
                    .filter(u -> u.getName().toLowerCase().contains(query) ||
                               (u.getDepartment() != null && u.getDepartment().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
        }

        // Get gamified metrics for all users
        List<GamifiedVelocityDTO> results = users.stream()
                .map(u -> getGamifiedVelocity(u.getUserId(), departmentFilter))
                .collect(Collectors.toList());

        // Apply filters
        if (minCompletionRate != null) {
            results = results.stream()
                    .filter(r -> r.getCompletionRate() >= minCompletionRate)
                    .collect(Collectors.toList());
        }
        if (maxCompletionRate != null) {
            results = results.stream()
                    .filter(r -> r.getCompletionRate() <= maxCompletionRate)
                    .collect(Collectors.toList());
        }
        if (minTasks != null) {
            results = results.stream()
                    .filter(r -> r.getTasksAssigned() >= minTasks)
                    .collect(Collectors.toList());
        }
        if (maxTasks != null) {
            results = results.stream()
                    .filter(r -> r.getTasksAssigned() <= maxTasks)
                    .collect(Collectors.toList());
        }
        if (performanceTier != null && !performanceTier.isEmpty()) {
            results = results.stream()
                    .filter(r -> performanceTier.equals(r.getPerformanceTier()))
                    .collect(Collectors.toList());
        }

        // Apply sorting
        if (sortBy != null && !sortBy.isEmpty()) {
            Comparator<GamifiedVelocityDTO> comparator = switch (sortBy.toLowerCase()) {
                case "name" -> Comparator.comparing(GamifiedVelocityDTO::getUserName);
                case "department" -> Comparator.comparing(GamifiedVelocityDTO::getDepartment);
                case "completionrate" -> Comparator.comparing(GamifiedVelocityDTO::getCompletionRate);
                case "tasksassigned" -> Comparator.comparing(GamifiedVelocityDTO::getTasksAssigned);
                case "taskscompleted" -> Comparator.comparing(GamifiedVelocityDTO::getTasksCompleted);
                case "score" -> Comparator.comparing(GamifiedVelocityDTO::getOverallScore);
                case "rank" -> Comparator.comparing(GamifiedVelocityDTO::getRank);
                default -> Comparator.comparing(GamifiedVelocityDTO::getOverallScore);
            };

            if ("desc".equalsIgnoreCase(sortOrder) || "descending".equalsIgnoreCase(sortOrder)) {
                comparator = comparator.reversed();
            }

            results.sort(comparator);
        } else {
            // Default sort: by overall score descending
            results.sort(Comparator.comparing(GamifiedVelocityDTO::getOverallScore).reversed());
        }

        // Recalculate ranks after filtering and sorting
        // Group by department for department ranks
        Map<String, List<GamifiedVelocityDTO>> byDepartment = results.stream()
                .filter(r -> r.getDepartment() != null && !r.getDepartment().isEmpty())
                .collect(Collectors.groupingBy(GamifiedVelocityDTO::getDepartment));

        // Update department ranks
        byDepartment.forEach((dept, deptResults) -> {
            deptResults.sort(Comparator.comparing(GamifiedVelocityDTO::getOverallScore).reversed());
            for (int i = 0; i < deptResults.size(); i++) {
                deptResults.get(i).setDepartmentRank(i + 1);
            }
        });

        // Update overall ranks
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }

        return results;
    }

    /**
     * Get all available departments
     */
    public List<String> getAllDepartments() {
        return userRepository.findAll().stream()
                .map(User::getDepartment)
                .filter(d -> d != null && !d.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}