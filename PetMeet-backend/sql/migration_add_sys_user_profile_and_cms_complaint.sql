-- 为新功能补齐缺失字段和数据表
-- 如果某个字段已经存在，MySQL 会报错，可先注释对应语句再重新执行
-- Windows 执行方式：
--   mysql -u root -p petmeet < sql\\migration_add_sys_user_profile_and_cms_complaint.sql

USE `petmeet`;

-- ---------- sys_user：补齐个人资料字段 ----------
SET @db := DATABASE();

-- sys_user.email
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'email'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `email` VARCHAR(100) DEFAULT NULL COMMENT ''邮箱''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- sys_user.gender
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'gender'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `gender` VARCHAR(20) DEFAULT NULL COMMENT ''性别：male/female/other''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- sys_user.birth_date
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'birth_date'
);
SET @sql := IF(
  @cnt = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `birth_date` DATE DEFAULT NULL COMMENT ''出生日期''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- cms_complaint：笔记投诉表 ----------
CREATE TABLE IF NOT EXISTS `cms_complaint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `note_id` BIGINT NOT NULL COMMENT '被投诉笔记 ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '上一级投诉记录 ID',
    `user_id` BIGINT NOT NULL COMMENT '投诉用户 ID',
    `reason` VARCHAR(50) NOT NULL COMMENT '投诉原因',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '投诉详情',
    `status` TINYINT DEFAULT 0 COMMENT '处理状态：0待处理，1已处理，2已驳回',
    `user_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '用户端可见标记：0显示，1用户已删除',
    `admin_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '管理端可见标记：0显示，1管理员已删除',
    `handle_remark` VARCHAR(500) DEFAULT NULL COMMENT '处理结果备注',
    `feedback_status` TINYINT DEFAULT 0 COMMENT '反馈状态：0未反馈，1满意，2不满意',
    `feedback_content` VARCHAR(500) DEFAULT NULL COMMENT '用户反馈内容',
    `feedback_time` DATETIME DEFAULT NULL COMMENT '用户反馈时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `handler_id` BIGINT DEFAULT NULL COMMENT '处理人 ID',
    PRIMARY KEY (`id`),
    KEY `idx_note` (`note_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记投诉表';
