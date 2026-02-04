package com.plantrack.backend.repository;

import com.plantrack.backend.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    // Find by entity type
    List<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType);
    
    // Find by user (performedBy)
    List<AuditLog> findByPerformedByOrderByTimestampDesc(String performedBy);
    
    // Find by action type
    List<AuditLog> findByActionOrderByTimestampDesc(String action);
    
    // Find by entity
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId);
    
    // Find by date range
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    List<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Find by user and date range
    @Query("SELECT a FROM AuditLog a WHERE a.performedBy = :performedBy AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    List<AuditLog> findByUserAndDateRange(@Param("performedBy") String performedBy, 
                                          @Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
}