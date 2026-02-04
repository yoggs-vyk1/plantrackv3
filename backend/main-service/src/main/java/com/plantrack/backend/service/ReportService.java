package com.plantrack.backend.service;

import com.plantrack.backend.model.*;
import com.plantrack.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private MilestoneRepository milestoneRepository;
    @Autowired
    private InitiativeRepository initiativeRepository;

    public Report generateDepartmentReport(String departmentName) {
        logger.info("Generating department report: department={}", departmentName);
        long startTime = System.currentTimeMillis();
        
        // 1. Find all Users in the Department
        List<User> deptUsers = userRepository.findAll().stream()
                .filter(u -> departmentName.equalsIgnoreCase(u.getDepartment()))
                .collect(Collectors.toList());

        if (deptUsers.isEmpty()) {
            logger.warn("No users found in department: department={}", departmentName);
            throw new RuntimeException("No users found in department: " + departmentName);
        }
        
        logger.debug("Found {} users in department: department={}", deptUsers.size(), departmentName);

        List<Long> userIds = deptUsers.stream().map(User::getUserId).collect(Collectors.toList());

        // 2. Calculate Avg Goal (Plan) Completion
        // (Assuming a simple: If status is COMPLETED = 100%, else 0% for Plans, or more complex logic)
        List<Plan> deptPlans = planRepository.findAll().stream()
                .filter(p -> userIds.contains(p.getUser().getUserId()))
                .collect(Collectors.toList());

        double avgGoal = deptPlans.isEmpty() ? 0.0 : deptPlans.stream()
                .mapToDouble(p -> "COMPLETED".equals(p.getStatus()) ? 100.0 : 0.0)
                .average().orElse(0.0);

        // 3. Calculate Avg Milestone Completion (Using the % field we just added!)
        List<Milestone> deptMilestones = milestoneRepository.findAll().stream()
                .filter(m -> userIds.contains(m.getPlan().getUser().getUserId()))
                .collect(Collectors.toList());

        double avgMilestone = deptMilestones.isEmpty() ? 0.0 : deptMilestones.stream()
                .mapToDouble(Milestone::getCompletionPercent)
                .average().orElse(0.0);

        // 4. Calculate Avg Initiative Completion
        // (Simple: COMPLETED = 100, others = 0)
        List<Initiative> deptInitiatives = initiativeRepository.findAll().stream()
                .filter(i -> i.getAssignedUsers() != null && !i.getAssignedUsers().isEmpty() &&
                        i.getAssignedUsers().stream()
                                .anyMatch(assignedUser -> userIds.contains(assignedUser.getUserId())))
                .collect(Collectors.toList());

        double avgInitiative = deptInitiatives.isEmpty() ? 0.0 : deptInitiatives.stream()
                .mapToDouble(i -> "COMPLETED".equals(i.getStatus()) ? 100.0 : 0.0)
                .average().orElse(0.0);

        // 5. Create and Save Report
        ReportMetrics metrics = new ReportMetrics(avgGoal, avgMilestone, avgInitiative, deptUsers.size());
        Report report = new Report(departmentName, metrics);

        Report savedReport = reportRepository.save(report);
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Department report generated: reportId={}, department={}, avgGoal={}%, avgMilestone={}%, avgInitiative={}%, duration={}ms", 
                savedReport.getReportId(), departmentName, avgGoal, avgMilestone, avgInitiative, duration);
        
        return savedReport;
    }

    public List<Report> getReportsByDepartment(String department) {
        logger.debug("Fetching reports for department: department={}", department);
        List<Report> reports = reportRepository.findByScopeOrderByGeneratedDateDesc(department);
        logger.info("Found {} reports for department: department={}", reports.size(), department);
        return reports;
    }
}