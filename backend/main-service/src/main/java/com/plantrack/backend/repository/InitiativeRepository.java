package com.plantrack.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plantrack.backend.model.Initiative;

public interface InitiativeRepository extends JpaRepository<Initiative, Long> {
    List<Initiative> findByMilestoneMilestoneId(Long milestoneId);
    
    @Query("SELECT DISTINCT i FROM Initiative i " +
           "LEFT JOIN FETCH i.milestone m " +
           "LEFT JOIN FETCH m.plan p " +
           "LEFT JOIN FETCH i.assignedUsers u " +
           "WHERE u.userId = :userId")
    List<Initiative> findByAssignedUserUserId(@Param("userId") Long userId);
}