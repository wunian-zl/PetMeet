-- ============================================
-- PetMeet 数据库字段更新脚本
-- 作用：补齐实体类新增但数据库表中缺失的字段
-- 执行方式：先切换到 petmeet 数据库，再执行本脚本
-- ============================================

-- 1. cms_note 表添加 is_sticky 字段
ALTER TABLE `cms_note`
ADD COLUMN `is_sticky` TINYINT(1) DEFAULT 0 COMMENT '是否置顶';

-- 1.1 cms_note 表添加视频笔记相关字段
ALTER TABLE `cms_note`
ADD COLUMN `type` VARCHAR(20) DEFAULT 'image' COMMENT '笔记类型：image/video',
ADD COLUMN `video_url` VARCHAR(255) DEFAULT NULL COMMENT '视频地址（type=video 时使用）';

-- 1.2 cms_note 表添加分类和标签字段
ALTER TABLE `cms_note`
ADD COLUMN `category` VARCHAR(50) DEFAULT NULL COMMENT '社区分类',
ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签（逗号分隔）';

-- 2. sys_user 表添加管理功能相关字段
ALTER TABLE `sys_user`
ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT '用户标签（CSV 格式）',
ADD COLUMN `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
ADD COLUMN `ban_reason` VARCHAR(255) DEFAULT NULL COMMENT '封禁原因',
ADD COLUMN `ban_time` DATETIME DEFAULT NULL COMMENT '封禁时间';

-- 3. oms_order 表添加发货和收货相关字段
ALTER TABLE `oms_order`
ADD COLUMN `ship_company` VARCHAR(50) DEFAULT NULL COMMENT '快递公司',
ADD COLUMN `tracking_no` VARCHAR(100) DEFAULT NULL COMMENT '快递单号',
ADD COLUMN `ship_time` DATETIME DEFAULT NULL COMMENT '发货时间',
ADD COLUMN `receiver` VARCHAR(50) DEFAULT NULL COMMENT '收货人姓名',
ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL COMMENT '收货人电话',
ADD COLUMN `address` VARCHAR(500) DEFAULT NULL COMMENT '收货地址';

-- 4. 如果 sys_follow 表不存在，则创建关注关系表
CREATE TABLE IF NOT EXISTS `sys_follow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `follower_id` BIGINT NOT NULL COMMENT '关注者 ID',
    `followee_id` BIGINT NOT NULL COMMENT '被关注者 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follow` (`follower_id`, `followee_id`),
    KEY `idx_followee` (`followee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注关系表';

SELECT '数据库字段更新完成！' AS message;

-- 管理员账号由AdminUserInitializer创建。
-- 首次启动时通过PETMEET_ADMIN_PASSWORD提供初始密码，
-- 此脚本不创建或重置管理员密码。

-- 6. sys_user 表补齐个人资料字段
ALTER TABLE `sys_user`
ADD COLUMN `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
ADD COLUMN `gender` VARCHAR(20) DEFAULT NULL COMMENT '性别',
ADD COLUMN `birth_date` DATE DEFAULT NULL COMMENT '出生日期';

-- 7. 如果 cms_complaint 表不存在，则创建笔记投诉表
CREATE TABLE IF NOT EXISTS `cms_complaint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `note_id` BIGINT NOT NULL COMMENT '被投诉笔记 ID',
    `user_id` BIGINT NOT NULL COMMENT '投诉用户 ID',
    `reason` VARCHAR(50) NOT NULL COMMENT '投诉原因',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '投诉详情',
    `status` TINYINT DEFAULT 0 COMMENT '处理状态：0待处理，1已处理，2已驳回',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `handler_id` BIGINT DEFAULT NULL COMMENT '处理人 ID',
    PRIMARY KEY (`id`),
    KEY `idx_note` (`note_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记投诉表';
