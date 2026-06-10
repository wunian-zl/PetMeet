-- 为管理端删除功能补齐管理员端和用户端的逻辑删除标记
-- Windows 执行方式：
--   mysql -u root -p petmeet < sql\migration_add_admin_delete_flags.sql

USE `petmeet`;

SET @db := DATABASE();

-- ---------- oms_order.user_deleted ----------
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_order' AND COLUMN_NAME = 'user_deleted'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `oms_order` ADD COLUMN `user_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''用户端逻辑删除标记：0未删除，1已删除''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE `oms_order` SET `user_deleted` = 0 WHERE `user_deleted` IS NULL;

-- ---------- oms_order.admin_deleted ----------
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_order' AND COLUMN_NAME = 'admin_deleted'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `oms_order` ADD COLUMN `admin_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''管理端逻辑删除标记：0未删除，1已删除''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE `oms_order` SET `admin_deleted` = 0 WHERE `admin_deleted` IS NULL;

-- ---------- cms_complaint.user_deleted ----------
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
UPDATE `cms_complaint` SET `user_deleted` = 0 WHERE `user_deleted` IS NULL;

-- ---------- cms_complaint.admin_deleted ----------
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
UPDATE `cms_complaint` SET `admin_deleted` = 0 WHERE `admin_deleted` IS NULL;

SELECT '逻辑删除标记补齐完成！' AS message;
