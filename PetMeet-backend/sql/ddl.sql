-- ==========================================
-- PetMeet完整数据库初始化脚本
-- 运行环境：MySQL 8.0 | InnoDB | utf8mb4
--
-- 使用说明：
-- 1.新环境只需要执行本文件，无需再逐个执行migration脚本。
-- 2.本文件会删除同名业务表并重新创建，请勿直接用于已有生产数据。
-- 3.管理员账号由后端AdminUserInitializer创建，不在SQL中保存默认密码。
-- ==========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `petmeet`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `petmeet`;

-- -------------------------------------------
-- 1. 用户表 sys_user
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户 ID，主键',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名，用于登录',
    `password` VARCHAR(255) NOT NULL COMMENT '密码，需加密存储',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称，前端展示用',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
    `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色：admin 为管理员，user 为普通用户',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '电子邮箱',
    `gender` VARCHAR(20) DEFAULT NULL COMMENT '性别：male/female/other',
    `birth_date` DATE DEFAULT NULL COMMENT '出生日期',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '用户标签（CSV 格式）',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `ban_reason` VARCHAR(255) DEFAULT NULL COMMENT '封禁原因',
    `ban_time` DATETIME DEFAULT NULL COMMENT '封禁时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -------------------------------------------
-- 1.1 关注关系表 sys_follow
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_follow`;
CREATE TABLE `sys_follow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `follower_id` BIGINT NOT NULL COMMENT '关注者 ID',
    `followee_id` BIGINT NOT NULL COMMENT '被关注者 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follow` (`follower_id`, `followee_id`),
    KEY `idx_followee` (`followee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注关系表';

-- -------------------------------------------
-- 2. 收货地址表 ums_address
-- -------------------------------------------
DROP TABLE IF EXISTS `ums_address`;
CREATE TABLE `ums_address` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '地址 ID，主键',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID，关联 sys_user.id',
    `name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    `phone` VARCHAR(20) NOT NULL COMMENT '收货人电话',
    `province` VARCHAR(50) NOT NULL COMMENT '省份',
    `city` VARCHAR(50) NOT NULL COMMENT '城市',
    `region` VARCHAR(50) NOT NULL COMMENT '区或县',
    `detail_address` VARCHAR(255) NOT NULL COMMENT '详细地址',
    `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址：0否，1是',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收货地址表';

-- -------------------------------------------
-- 3. 商品分类表 pms_category
-- -------------------------------------------
DROP TABLE IF EXISTS `pms_category`;
CREATE TABLE `pms_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类 ID，主键',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(500) DEFAULT NULL COMMENT '分类图标地址',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- -------------------------------------------
-- 4. 商品表 pms_product
-- -------------------------------------------
DROP TABLE IF EXISTS `pms_product`;
CREATE TABLE `pms_product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品 ID，主键',
    `category_id` BIGINT NOT NULL COMMENT '分类 ID，关联 pms_category.id',
    `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `sub_title` VARCHAR(255) DEFAULT NULL COMMENT '副标题或卖点',
    `price` DECIMAL(10,2) NOT NULL COMMENT '商品价格，单位为元',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位，如 kg、包、个',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，用于并发库存扣减',
    `cover_img` VARCHAR(500) NOT NULL COMMENT '商品封面图地址',
    `cover_imgs` JSON DEFAULT NULL COMMENT '商品主图组，JSON 数组格式',
    `detail_imgs` JSON DEFAULT NULL COMMENT '商品详情图，JSON 数组格式',
    `description` TEXT DEFAULT NULL COMMENT '商品描述，富文本 HTML 格式',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0下架，1上架',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `warning_stock` INT DEFAULT NULL COMMENT '预警库存',
    `sort_weight` INT DEFAULT NULL COMMENT '排序权重',
    `sales` INT NOT NULL DEFAULT 0 COMMENT '销量',
    `pet_type` VARCHAR(20) DEFAULT NULL COMMENT '适用宠物：cat/dog/general',
    `views` INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    `related_note_count` INT NOT NULL DEFAULT 0 COMMENT '关联笔记数',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- -------------------------------------------
-- 5. 笔记表 cms_note
-- -------------------------------------------
DROP TABLE IF EXISTS `cms_note`;
CREATE TABLE `cms_note` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '笔记 ID，主键',
    `user_id` BIGINT NOT NULL COMMENT '作者 ID，关联 sys_user.id',
    `title` VARCHAR(100) NOT NULL COMMENT '笔记标题',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '社区分类',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签（逗号分隔）',
    `content` TEXT NOT NULL COMMENT '笔记内容，富文本 HTML 格式',
    `cover_img` VARCHAR(500) DEFAULT NULL COMMENT '封面图地址，视频笔记也使用该字段',
    `images` JSON DEFAULT NULL COMMENT '笔记图片列表，JSON 数组格式',
    `type` VARCHAR(20) NOT NULL DEFAULT 'image' COMMENT '笔记类型：image/video',
    `video_url` VARCHAR(500) DEFAULT NULL COMMENT '视频地址（type=video 时使用）',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `collect_count` INT NOT NULL DEFAULT 0 COMMENT '收藏数',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待审核，1已发布，2审核拒绝',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1删除',
    `is_recommended` TINYINT(1) DEFAULT 0 COMMENT '是否推荐',
    `is_sticky` TINYINT(1) DEFAULT 0 COMMENT '是否置顶',
    `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `audit_user_id` BIGINT DEFAULT NULL COMMENT '审核操作人用户 ID',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '审核驳回原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记表';

-- -------------------------------------------
-- 6. 评论表 cms_comment
-- -------------------------------------------
DROP TABLE IF EXISTS `cms_comment`;
CREATE TABLE `cms_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论 ID，主键',
    `note_id` BIGINT NOT NULL COMMENT '笔记 ID，关联 cms_note.id',
    `user_id` BIGINT NOT NULL COMMENT '评论用户 ID，关联 sys_user.id',
    `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_note_id` (`note_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- -------------------------------------------
-- 7. 笔记商品关联表 cms_note_product_relation
-- 作用：承接内容带货链路，让笔记可以关联商品
-- -------------------------------------------
DROP TABLE IF EXISTS `cms_note_product_relation`;
CREATE TABLE `cms_note_product_relation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联 ID，主键',
    `note_id` BIGINT NOT NULL COMMENT '笔记 ID，关联 cms_note.id',
    `product_id` BIGINT NOT NULL COMMENT '商品 ID，关联 pms_product.id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_note_product` (`note_id`, `product_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记商品关联表';

-- -------------------------------------------
-- 8. 互动记录表 sys_interaction
-- 作用：用于前端回显用户的点赞和收藏状态
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_interaction`;
CREATE TABLE `sys_interaction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '互动 ID，主键',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID，关联 sys_user.id',
    `target_id` BIGINT NOT NULL COMMENT '目标 ID，按 type 指向笔记或商品',
    `type` TINYINT NOT NULL COMMENT '互动类型：1点赞笔记，2收藏笔记',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target_type` (`user_id`, `target_id`, `type`),
    KEY `idx_target_id` (`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='互动记录表';

-- -------------------------------------------
-- 9. 购物车表 oms_cart_item
-- -------------------------------------------
DROP TABLE IF EXISTS `oms_cart_item`;
CREATE TABLE `oms_cart_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车项 ID，主键',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID，关联 sys_user.id',
    `product_id` BIGINT NOT NULL COMMENT '商品 ID，关联 pms_product.id',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '商品数量',
    `selected` TINYINT NOT NULL DEFAULT 1 COMMENT '是否选中：1选中，0未选中',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入购物车时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- -------------------------------------------
-- 10. 订单表 oms_order
-- -------------------------------------------
DROP TABLE IF EXISTS `oms_order`;
CREATE TABLE `oms_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单 ID，主键',
    `order_sn` VARCHAR(64) NOT NULL COMMENT '订单编号，需保证唯一',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID，关联 sys_user.id',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额，单位为元',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待付款，1已付款，2已发货，3已完成，4已关闭',
    `review_status` TINYINT NOT NULL DEFAULT 0 COMMENT '评价状态：0待评价，1已评价',
    `review_score` TINYINT DEFAULT NULL COMMENT '评价星级（1-5）',
    `review_content` VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
    `review_time` DATETIME DEFAULT NULL COMMENT '评价时间',
    `receiver_info` JSON DEFAULT NULL COMMENT '收货信息快照，包含姓名、电话、地址等',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `ship_company` VARCHAR(100) DEFAULT NULL COMMENT '物流公司',
    `tracking_no` VARCHAR(100) DEFAULT NULL COMMENT '物流单号',
    `ship_time` DATETIME DEFAULT NULL COMMENT '发货时间',
    `receiver` VARCHAR(50) DEFAULT NULL COMMENT '收货人姓名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '收货人电话',
    `address` VARCHAR(500) DEFAULT NULL COMMENT '收货地址',
    `user_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '用户端逻辑删除标记：0未删除，1已删除',
    `admin_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '管理端逻辑删除标记：0未删除，1已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_sn` (`order_sn`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- -------------------------------------------
-- 11. 订单明细表 oms_order_item
-- -------------------------------------------
DROP TABLE IF EXISTS `oms_order_item`;
CREATE TABLE `oms_order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单明细 ID，主键',
    `order_id` BIGINT NOT NULL COMMENT '订单 ID，关联 oms_order.id',
    `product_id` BIGINT NOT NULL COMMENT '商品 ID，关联 pms_product.id',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称快照',
    `product_img` VARCHAR(500) NOT NULL COMMENT '商品图片快照',
    `price` DECIMAL(10,2) NOT NULL COMMENT '商品单价快照，单位为元',
    `quantity` INT NOT NULL COMMENT '购买数量',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

-- -------------------------------------------
-- 12. 订单售后申请表 oms_after_sale
-- -------------------------------------------
DROP TABLE IF EXISTS `oms_after_sale`;
CREATE TABLE `oms_after_sale` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '售后申请 ID',
    `order_id` BIGINT NOT NULL COMMENT '订单 ID',
    `order_item_id` BIGINT NOT NULL COMMENT '订单商品明细 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `type` TINYINT NOT NULL DEFAULT 0 COMMENT '售后类型：0仅退款，1退货退款，2换货',
    `reason` VARCHAR(100) DEFAULT NULL COMMENT '申请原因',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '问题描述',
    `evidence_images` TEXT DEFAULT NULL COMMENT '退款凭证图片列表（JSON 数组）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0申请中，1处理中，2已完成，3已拒绝，4已取消',
    `user_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '用户端逻辑删除标记：0未删除，1已删除',
    `admin_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '管理端逻辑删除标记：0未删除，1已删除',
    `handle_remark` VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_item_id` (`order_item_id`),
    KEY `idx_status` (`status`),
    KEY `idx_order_status` (`order_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单售后申请表';

-- -------------------------------------------
-- 13. 内容投诉表 cms_complaint
-- -------------------------------------------
DROP TABLE IF EXISTS `cms_complaint`;
CREATE TABLE `cms_complaint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '投诉 ID',
    `note_id` BIGINT NOT NULL COMMENT '被投诉笔记 ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '上一级投诉 ID，用于再次投诉',
    `user_id` BIGINT NOT NULL COMMENT '投诉用户 ID',
    `reason` VARCHAR(50) NOT NULL COMMENT '投诉原因',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '投诉详情',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待处理，1已处理，2已驳回',
    `handle_remark` VARCHAR(500) DEFAULT NULL COMMENT '管理员处理结果',
    `user_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '用户端逻辑删除标记：0未删除，1已删除',
    `admin_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '管理端逻辑删除标记：0未删除，1已删除',
    `feedback_status` TINYINT NOT NULL DEFAULT 0 COMMENT '反馈状态：0未反馈，1满意，2不满意',
    `feedback_content` VARCHAR(500) DEFAULT NULL COMMENT '用户反馈内容',
    `feedback_time` DATETIME DEFAULT NULL COMMENT '用户反馈时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投诉时间',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `handler_id` BIGINT DEFAULT NULL COMMENT '处理管理员 ID',
    PRIMARY KEY (`id`),
    KEY `idx_note` (`note_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_parent` (`parent_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容投诉表';

-- -------------------------------------------
-- 14. 系统通知表 sys_notification
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_notification`;
CREATE TABLE `sys_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知 ID',
    `user_id` BIGINT NOT NULL COMMENT '接收用户 ID',
    `title` VARCHAR(100) NOT NULL COMMENT '通知标题',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '通知内容',
    `biz_type` VARCHAR(30) DEFAULT NULL COMMENT '业务类型，如note、complaint、order',
    `biz_id` BIGINT DEFAULT NULL COMMENT '关联业务 ID',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态：0未读，1已读',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `read_time` DATETIME DEFAULT NULL COMMENT '读取时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

-- -------------------------------------------
-- 15. 养宠科普栏目配置表 cms_banner
-- 说明：商城顶部展示科普卡片，点击后跳转社区并按关键词自动搜索
-- -------------------------------------------
DROP TABLE IF EXISTS `cms_banner`;
CREATE TABLE `cms_banner` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    `title` VARCHAR(100) NULL COMMENT '卡片标题',
    `position` VARCHAR(50) NOT NULL COMMENT '展示位置编码，如 SHOP_TOP',
    `slot` VARCHAR(20) NOT NULL COMMENT '槽位类型，建议固定为 card',
    `image_url` VARCHAR(255) NOT NULL COMMENT '图片地址',
    `keyword` VARCHAR(50) NULL COMMENT '预设关键词，用于社区自动搜索',
    `link_url` VARCHAR(255) NULL COMMENT '兼容字段，不再作为主跳转逻辑',
    `link_type` VARCHAR(20) DEFAULT 'internal' COMMENT '兼容字段，表示跳转类型',
    `sort` INT DEFAULT 0 COMMENT '排序值',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0禁用，1启用',
    `start_time` DATETIME NULL COMMENT '开始时间，可为空',
    `end_time` DATETIME NULL COMMENT '结束时间，可为空',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_position_status` (`position`, `status`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='养宠科普栏目配置表';

-- 示例数据：商城顶部的养宠科普卡片
INSERT INTO `cms_banner` (`title`, `position`, `slot`, `image_url`, `keyword`, `link_url`, `link_type`, `sort`, `status`)
VALUES
    ('新手养狗指南', 'SHOP_TOP', 'card', '/petmeetImage/Main-Commodity-image/10025.jpg', '幼犬', '/', 'internal', 1, 1),
    ('猫咪绝育必读', 'SHOP_TOP', 'card', '/petmeetImage/Main-Commodity-image/10088.jpg', '绝育', '/', 'internal', 2, 1),
    ('科学换粮指南', 'SHOP_TOP', 'card', '/petmeetImage/Main-Commodity-image/10069.jpg', '换粮', '/', 'internal', 3, 1),
    ('猫狗驱虫全攻略', 'SHOP_TOP', 'card', '/petmeetImage/Main-Commodity-image/10032.jpg', '驱虫', '/', 'internal', 4, 1);

SET FOREIGN_KEY_CHECKS = 1;

-- ==========================================
-- 建表完成
-- 表清单：
--   1. sys_user                   用户表
--   2. sys_follow                 关注关系表
--   3. ums_address                收货地址表
--   4. pms_category               商品分类表
--   5. pms_product                商品表
--   6. cms_note                   笔记表
--   7. cms_comment                评论表
--   8. cms_note_product_relation  笔记商品关联表
--   9. sys_interaction            互动记录表
--   10. oms_cart_item             购物车表
--   11. oms_order                 订单表
--   12. oms_order_item            订单明细表
--   13. oms_after_sale            订单售后申请表
--   14. cms_complaint             内容投诉表
--   15. sys_notification          系统通知表
--   16. cms_banner                养宠科普栏目配置表
-- ==========================================
