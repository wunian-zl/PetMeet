-- 为投诉流程补齐处理备注、用户反馈和再次投诉链路字段
-- Windows 执行方式：
--   mysql -u root -p petmeet < sql\\migration_extend_cms_complaint_feedback.sql

USE `petmeet`;

SET @db := DATABASE();

-- cms_complaint.parent_id
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_complaint' AND COLUMN_NAME = 'parent_id'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `cms_complaint` ADD COLUMN `parent_id` BIGINT DEFAULT NULL COMMENT ''上一级投诉记录 ID'' AFTER `note_id`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- cms_complaint.handle_remark
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_complaint' AND COLUMN_NAME = 'handle_remark'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `cms_complaint` ADD COLUMN `handle_remark` VARCHAR(500) DEFAULT NULL COMMENT ''处理结果备注''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- cms_complaint.feedback_status
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_complaint' AND COLUMN_NAME = 'feedback_status'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `cms_complaint` ADD COLUMN `feedback_status` TINYINT DEFAULT 0 COMMENT ''反馈状态：0未反馈，1满意，2不满意''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- cms_complaint.feedback_content
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_complaint' AND COLUMN_NAME = 'feedback_content'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `cms_complaint` ADD COLUMN `feedback_content` VARCHAR(500) DEFAULT NULL COMMENT ''用户反馈内容''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- cms_complaint.feedback_time
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_complaint' AND COLUMN_NAME = 'feedback_time'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `cms_complaint` ADD COLUMN `feedback_time` DATETIME DEFAULT NULL COMMENT ''用户反馈时间''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- cms_complaint.user_deleted
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_complaint' AND COLUMN_NAME = 'user_deleted'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `cms_complaint` ADD COLUMN `user_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''用户端可见标记：0显示，1用户已删除''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- cms_complaint.admin_deleted
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_complaint' AND COLUMN_NAME = 'admin_deleted'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `cms_complaint` ADD COLUMN `admin_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''管理端可见标记：0显示，1管理员已删除''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- idx_parent（用于再次投诉链路查询）
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_complaint' AND INDEX_NAME = 'idx_parent'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `cms_complaint` ADD KEY `idx_parent` (`parent_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '投诉链路字段补齐完成！' AS message;
