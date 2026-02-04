package com.plantrack.backend.repository;

import com.plantrack.backend.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    // Custom method to find all plans for a specific user
    List<Plan> findByUserUserId(Long userId);
    
    // Query to find plans that have initiatives assigned to a specific user
    @Query("SELECT DISTINCT p FROM Plan p " +
           "JOIN p.milestones m " +
           "JOIN m.initiatives i " +
           "JOIN i.assignedUsers u " +
           "WHERE u.userId = :userId")
    List<Plan> findPlansWithAssignedInitiatives(@Param("userId") Long userId);
}