-- Migration Script: Convert Single Assignee to Multiple Assignees
-- This script migrates existing initiative assignments from the single assigned_user_id column
-- to the new initiative_assignees join table

-- Step 1: Create the join table (if not already created by Hibernate)
CREATE TABLE IF NOT EXISTS initiative_assignees (
    initiative_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (initiative_id, user_id),
    FOREIGN KEY (initiative_id) REFERENCES initiatives(initiative_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_initiative_id (initiative_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 2: Migrate existing data from assigned_user_id to join table
-- Only migrate if the join table is empty and initiatives table has data
INSERT INTO initiative_assignees (initiative_id, user_id)
SELECT initiative_id, assigned_user_id
FROM initiatives
WHERE assigned_user_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM initiative_assignees ia 
    WHERE ia.initiative_id = initiatives.initiative_id
  );

-- Step 3: Verify migration
-- Run this query to check if all initiatives have been migrated:
-- SELECT 
--     (SELECT COUNT(*) FROM initiatives WHERE assigned_user_id IS NOT NULL) as old_count,
--     (SELECT COUNT(*) FROM initiative_assignees) as new_count;

-- Step 4: (OPTIONAL - Run after verifying migration is successful)
-- Remove the old assigned_user_id column
-- WARNING: Only run this after verifying the migration is complete and the application is working correctly
-- ALTER TABLE initiatives DROP FOREIGN KEY IF EXISTS fk_assigned_user;
-- ALTER TABLE initiatives DROP COLUMN IF EXISTS assigned_user_id;

