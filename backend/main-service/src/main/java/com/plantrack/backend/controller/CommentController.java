package com.plantrack.backend.controller;

import com.plantrack.backend.model.Comment;
import com.plantrack.backend.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * Create a new comment on an initiative
     */
    @PostMapping("/initiatives/{initiativeId}/comments")
    public Comment createComment(@PathVariable Long initiativeId, @Valid @RequestBody Comment comment) {
        return commentService.createComment(initiativeId, comment);
    }

    /**
     * Get all comments for an initiative
     */
    @GetMapping("/initiatives/{initiativeId}/comments")
    public List<Comment> getComments(@PathVariable Long initiativeId) {
        return commentService.getCommentsByInitiative(initiativeId);
    }

    /**
     * Update a comment
     */
    @PutMapping("/comments/{commentId}")
    public Comment updateComment(@PathVariable Long commentId, @Valid @RequestBody Comment comment) {
        return commentService.updateComment(commentId, comment);
    }

    /**
     * Delete a comment (soft delete)
     */
    @DeleteMapping("/comments/{commentId}")
    public void deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
    }
}
