package com.plantrack.backend.repository;

import com.plantrack.backend.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // Find all non-deleted comments for an initiative, ordered by creation date (newest first)
    @Query("SELECT c FROM Comment c " +
           "LEFT JOIN FETCH c.author " +
           "LEFT JOIN FETCH c.mentionedUsers " +
           "WHERE c.initiative.initiativeId = :initiativeId AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    List<Comment> findByInitiativeIdOrderByCreatedAtDesc(@Param("initiativeId") Long initiativeId);
    
    // Find all comments by a specific user
    @Query("SELECT c FROM Comment c " +
           "LEFT JOIN FETCH c.initiative " +
           "LEFT JOIN FETCH c.mentionedUsers " +
           "WHERE c.author.userId = :userId AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    List<Comment> findByAuthorUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    // Count comments for an initiative (excluding deleted)
    Long countByInitiativeInitiativeIdAndDeletedFalse(Long initiativeId);
}
