package com.plantrack.backend.controller;

import com.plantrack.backend.model.Report;
import com.plantrack.backend.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    // Generate a new report for a department
    // POST /api/reports/generate?department=IT
    @PostMapping("/generate")
    public Report generateReport(@RequestParam String department) {
        return reportService.generateDepartmentReport(department);
    }

    // Get history of reports for a department
    // GET /api/reports?department=IT
    @GetMapping
    public List<Report> getReports(@RequestParam String department) {
        return reportService.getReportsByDepartment(department);
    }
}