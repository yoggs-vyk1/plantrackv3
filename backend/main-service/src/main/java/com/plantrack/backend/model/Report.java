package com.plantrack.backend.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@EntityListeners(AuditingEntityListener.class)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Column(nullable = false)
    private String scope; // e.g., "IT", "HR", "SALES"

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedDate;

    // --- OPTIMAL DATA STRUCTURE: EMBEDDED METRICS ---
    // This groups the stats in Java but saves them as columns in the same table.
    @Embedded
    private ReportMetrics metrics;

    public Report() {}

    public Report(String scope, ReportMetrics metrics) {
        this.scope = scope;
        this.metrics = metrics;
    }

    // Getters and Setters
    public Long getReportId() { return reportId; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public LocalDateTime getGeneratedDate() { return generatedDate; }
    public ReportMetrics getMetrics() { return metrics; }
    public void setMetrics(ReportMetrics metrics) { this.metrics = metrics; }
}