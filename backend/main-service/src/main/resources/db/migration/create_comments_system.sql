-- Migration Script: Create Comments System with @Mention Feature
-- This script creates the comments table and comment_mentions join table

-- Step 1: Create the comments table
CREATE TABLE IF NOT EXISTS comments (
    comment_id BIGINT NOT NULL AUTO_INCREMENT,
    content VARCHAR(2000) NOT NULL,
    initiative_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (comment_id),
    FOREIGN KEY (initiative_id) REFERENCES initiatives(initiative_id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_initiative_id (initiative_id),
    INDEX idx_author_id (author_id),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 2: Create the comment_mentions join table for @mention tracking
CREATE TABLE IF NOT EXISTS comment_mentions (
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (comment_id, user_id),
    FOREIGN KEY (comment_id) REFERENCES comments(comment_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_comment_id (comment_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 3: Verify tables were created
-- Run these queries to verify:
-- SHOW TABLES LIKE 'comments';
-- SHOW TABLES LIKE 'comment_mentions';
-- DESCRIBE comments;
-- DESCRIBE comment_mentions;

