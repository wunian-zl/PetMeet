-- Extend comments into two-level threads and allow comment complaints.
-- Usage:
--   mysql -u root -p petmeet < sql/migration_extend_cms_comment_thread.sql

SET @db = DATABASE();

-- ---------- cms_comment thread fields ----------
SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_comment' AND COLUMN_NAME = 'parent_id'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE `cms_comment` ADD COLUMN `parent_id` BIGINT DEFAULT NULL COMMENT ''一级评论 ID，NULL 表示一级评论'' AFTER `note_id`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_comment' AND COLUMN_NAME = 'reply_to_id'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE `cms_comment` ADD COLUMN `reply_to_id` BIGINT DEFAULT NULL COMMENT ''被回复的评论 ID'' AFTER `parent_id`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_comment' AND COLUMN_NAME = 'like_count'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE `cms_comment` ADD COLUMN `like_count` INT NOT NULL DEFAULT 0 COMMENT ''评论点赞数'' AFTER `content`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_comment' AND COLUMN_NAME = 'status'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE `cms_comment` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT ''状态：0正常，1已删除'' AFTER `like_count`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_comment' AND COLUMN_NAME = 'delete_time'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE `cms_comment` ADD COLUMN `delete_time` DATETIME DEFAULT NULL COMMENT ''删除时间'' AFTER `create_time`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `cms_comment` SET `like_count` = 0 WHERE `like_count` IS NULL;
UPDATE `cms_comment` SET `status` = 0 WHERE `status` IS NULL;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_comment' AND INDEX_NAME = 'idx_parent_id'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE `cms_comment` ADD INDEX `idx_parent_id` (`parent_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_comment' AND INDEX_NAME = 'idx_reply_to_id'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE `cms_comment` ADD INDEX `idx_reply_to_id` (`reply_to_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_comment' AND INDEX_NAME = 'idx_note_parent_status_time'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE `cms_comment` ADD INDEX `idx_note_parent_status_time` (`note_id`, `parent_id`, `status`, `create_time`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- cms_complaint target fields ----------
SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_complaint' AND COLUMN_NAME = 'target_type'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE `cms_complaint` ADD COLUMN `target_type` VARCHAR(20) NOT NULL DEFAULT ''note'' COMMENT ''投诉对象类型：note笔记，comment评论'' AFTER `note_id`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_complaint' AND COLUMN_NAME = 'comment_id'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE `cms_complaint` ADD COLUMN `comment_id` BIGINT DEFAULT NULL COMMENT ''被投诉评论 ID'' AFTER `target_type`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `cms_complaint` SET `target_type` = 'note' WHERE `target_type` IS NULL OR `target_type` = '';

SET @exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_complaint' AND INDEX_NAME = 'idx_target'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE `cms_complaint` ADD INDEX `idx_target` (`target_type`, `note_id`, `comment_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
