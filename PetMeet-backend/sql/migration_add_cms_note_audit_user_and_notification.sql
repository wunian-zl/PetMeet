-- 为笔记审核补齐审核人字段，并新增系统通知表
-- 执行方式：
--   mysql -u root -p petmeet < sql\\migration_add_cms_note_audit_user_and_notification.sql

USE `petmeet`;

SET @db := DATABASE();

-- ---------- cms_note.audit_user_id ----------
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cms_note' AND COLUMN_NAME = 'audit_user_id'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `cms_note` ADD COLUMN `audit_user_id` BIGINT DEFAULT NULL COMMENT ''审核操作人用户 ID''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- sys_notification ----------
CREATE TABLE IF NOT EXISTS `sys_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `user_id` BIGINT NOT NULL COMMENT '接收通知的用户 ID',
    `title` VARCHAR(100) NOT NULL COMMENT '通知标题',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '通知内容',
    `biz_type` VARCHAR(30) DEFAULT NULL COMMENT '业务类型，如笔记、资料、投诉等',
    `biz_id` BIGINT DEFAULT NULL COMMENT '关联业务 ID',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态：0未读，1已读',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `read_time` DATETIME DEFAULT NULL COMMENT '读取时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`,`is_read`),
    KEY `idx_user_time` (`user_id`,`create_time`),
    KEY `idx_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';
