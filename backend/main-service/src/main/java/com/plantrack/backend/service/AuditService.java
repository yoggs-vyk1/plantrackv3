package com.plantrack.backend.service;

import com.plantrack.backend.model.AuditLog;
import com.plantrack.backend.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Log a CREATE operation
     */
    public void logCreate(String entityType, Long entityId, String details) {
        String performedBy = getCurrentUser();
        logger.debug("Creating audit log: action=CREATE, entityType={}, entityId={}, performedBy={}", 
                entityType, entityId, performedBy);
        AuditLog log = new AuditLog("CREATE", performedBy, entityType, entityId, details);
        auditLogRepository.save(log);
        logger.trace("Audit log created: auditLogId={}, entityType={}, entityId={}", 
                log.getId(), entityType, entityId);
    }

    /**
     * Log an UPDATE operation
     */
    public void logUpdate(String entityType, Long entityId, String details) {
        String performedBy = getCurrentUser();
        logger.debug("Creating audit log: action=UPDATE, entityType={}, entityId={}, performedBy={}", 
                entityType, entityId, performedBy);
        AuditLog log = new AuditLog("UPDATE", performedBy, entityType, entityId, details);
        auditLogRepository.save(log);
        logger.trace("Audit log created: auditLogId={}, entityType={}, entityId={}", 
                log.getId(), entityType, entityId);
    }

    /**
     * Log a DELETE operation
     */
    public void logDelete(String entityType, Long entityId, String details) {
        String performedBy = getCurrentUser();
        logger.info("Creating audit log: action=DELETE, entityType={}, entityId={}, performedBy={}", 
                entityType, entityId, performedBy);
        AuditLog log = new AuditLog("DELETE", performedBy, entityType, entityId, details);
        auditLogRepository.save(log);
        logger.trace("Audit log created: auditLogId={}, entityType={}, entityId={}", 
                log.getId(), entityType, entityId);
    }

    /**
     * Log a status change with old and new values
     */
    public void logStatusChange(String entityType, Long entityId, String oldStatus, String newStatus, String details) {
        String performedBy = getCurrentUser();
        logger.info("Creating audit log: action=UPDATE_STATUS, entityType={}, entityId={}, status={}->{}, performedBy={}", 
                entityType, entityId, oldStatus, newStatus, performedBy);
        AuditLog log = new AuditLog("UPDATE_STATUS", performedBy, entityType, entityId, details);
        log.setOldValue(oldStatus);
        log.setNewValue(newStatus);
        auditLogRepository.save(log);
        logger.trace("Audit log created: auditLogId={}, entityType={}, entityId={}", 
                log.getId(), entityType, entityId);
    }

    /**
     * Get current authenticated user email or ID
     */
    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String user = auth.getName(); // Returns email or username
            logger.trace("Retrieved current user from security context: user={}", user);
            return user;
        }
        logger.trace("No authenticated user found, using SYSTEM");
        return "SYSTEM";
    }
}

