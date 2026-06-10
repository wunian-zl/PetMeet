-- 为管理端内容审核补齐统计和筛选需要的审核字段
-- 执行方式：
--   mysql -u root -p petmeet < sql\\migration_add_cms_note_audit_fields.sql

USE `petmeet`;

SET @db := DATABASE();

-- cms_note.audit_time
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_note' AND COLUMN_NAME = 'audit_time'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `cms_note` ADD COLUMN `audit_time` DATETIME DEFAULT NULL COMMENT ''审核时间''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- cms_note.reject_reason
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_note' AND COLUMN_NAME = 'reject_reason'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `cms_note` ADD COLUMN `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT ''驳回原因''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
