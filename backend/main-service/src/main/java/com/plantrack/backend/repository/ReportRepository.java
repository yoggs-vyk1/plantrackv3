package com.plantrack.backend.repository;

import com.plantrack.backend.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    // Find reports for a specific department, ordered by newest first
    List<Report> findByScopeOrderByGeneratedDateDesc(String scope);
}