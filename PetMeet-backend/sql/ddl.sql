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
    `parent_id` BIGINT DEFAULT NULL COMMENT '一级评论 ID，NULL 表示一级评论',
    `reply_to_id` BIGINT DEFAULT NULL COMMENT '被回复的评论 ID',
    `user_id` BIGINT NOT NULL COMMENT '评论用户 ID，关联 sys_user.id',
    `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '评论点赞数',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0正常，1已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `delete_time` DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_note_id` (`note_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_reply_to_id` (`reply_to_id`),
    KEY `idx_note_parent_status_time` (`note_id`, `parent_id`, `status`, `create_time`)
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
    `refund_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '已退款金额',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待付款，1已付款，2已发货，3已完成，4已关闭，5退款中',
    `pay_type` TINYINT DEFAULT NULL COMMENT '支付方式：1支付宝，2微信Mock',
    `pay_sn` VARCHAR(64) DEFAULT NULL COMMENT '系统支付流水号',
    `trade_no` VARCHAR(128) DEFAULT NULL COMMENT '第三方交易流水号',
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
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '订单备注',
    `user_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '用户端逻辑删除标记：0未删除，1已删除',
    `admin_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '管理端逻辑删除标记：0未删除，1已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_sn` (`order_sn`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_pay_sn` (`pay_sn`),
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
-- 12. 支付流水表 oms_pay_log
-- -------------------------------------------
DROP TABLE IF EXISTS `oms_pay_log`;
CREATE TABLE `oms_pay_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付流水 ID',
    `pay_sn` VARCHAR(64) NOT NULL COMMENT '系统支付流水号',
    `order_id` BIGINT NOT NULL COMMENT '订单 ID',
    `order_sn` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `pay_type` TINYINT NOT NULL COMMENT '支付方式：1支付宝，2微信Mock',
    `pay_mode` TINYINT NOT NULL DEFAULT 1 COMMENT '支付模式：1扫码',
    `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付，1成功，2失败，3关闭',
    `trade_no` VARCHAR(128) DEFAULT NULL COMMENT '第三方交易流水号',
    `qr_code_url` VARCHAR(500) DEFAULT NULL COMMENT '二维码内容',
    `pay_page_url` TEXT DEFAULT NULL COMMENT '支付页面内容',
    `expire_time` DATETIME NOT NULL COMMENT '支付过期时间',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付完成时间',
    `callback_time` DATETIME DEFAULT NULL COMMENT '回调时间',
    `callback_content` TEXT DEFAULT NULL COMMENT '回调原始报文',
    `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_sn` (`pay_sn`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_sn` (`order_sn`),
    KEY `idx_user_id` (`user_id`),
    UNIQUE KEY `uk_pay_trade_no` (`pay_type`, `trade_no`),
    KEY `idx_pay_status` (`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付流水表';

-- -------------------------------------------
-- 13. 退款流水表 oms_refund_log
-- -------------------------------------------
DROP TABLE IF EXISTS `oms_refund_log`;
CREATE TABLE `oms_refund_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '退款流水 ID',
    `refund_sn` VARCHAR(64) NOT NULL COMMENT '退款流水号',
    `order_id` BIGINT NOT NULL COMMENT '订单 ID',
    `order_sn` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `pay_log_id` BIGINT DEFAULT NULL COMMENT '支付流水 ID',
    `after_sale_id` BIGINT DEFAULT NULL COMMENT '售后申请 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `pay_type` TINYINT DEFAULT NULL COMMENT '原支付方式：1支付宝，2微信Mock',
    `refund_amount` DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    `refund_reason` VARCHAR(200) DEFAULT NULL COMMENT '退款原因',
    `refund_status` TINYINT NOT NULL DEFAULT 0 COMMENT '退款状态：0退款中，1成功，2失败',
    `trade_no` VARCHAR(128) DEFAULT NULL COMMENT '第三方退款交易号',
    `refund_time` DATETIME DEFAULT NULL COMMENT '退款完成时间',
    `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refund_sn` (`refund_sn`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_after_sale_id` (`after_sale_id`),
    KEY `idx_refund_status` (`refund_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款流水表';

-- -------------------------------------------
-- 14. 订单售后申请表 oms_after_sale
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
    `refund_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '售后退款金额',
    `return_address` VARCHAR(500) DEFAULT NULL COMMENT '商家退货地址',
    `return_company` VARCHAR(100) DEFAULT NULL COMMENT '买家退货物流公司',
    `return_tracking_no` VARCHAR(100) DEFAULT NULL COMMENT '买家退货物流单号',
    `return_ship_time` DATETIME DEFAULT NULL COMMENT '买家退货发出时间',
    `return_receive_time` DATETIME DEFAULT NULL COMMENT '商家确认收货时间',
    `exchange_company` VARCHAR(100) DEFAULT NULL COMMENT '换货物流公司',
    `exchange_tracking_no` VARCHAR(100) DEFAULT NULL COMMENT '换货物流单号',
    `exchange_ship_time` DATETIME DEFAULT NULL COMMENT '换货发货时间',
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
    KEY `idx_order_status` (`order_id`, `status`),
    KEY `idx_type_status` (`type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单售后申请表';

-- -------------------------------------------
-- 15. 售后操作日志表 oms_after_sale_log
-- -------------------------------------------
DROP TABLE IF EXISTS `oms_after_sale_log`;
CREATE TABLE `oms_after_sale_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '售后操作日志 ID',
    `after_sale_id` BIGINT NOT NULL COMMENT '售后申请 ID',
    `from_status` TINYINT DEFAULT NULL COMMENT '变更前状态',
    `to_status` TINYINT NOT NULL COMMENT '变更后状态',
    `action` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `operator_type` VARCHAR(20) NOT NULL COMMENT '操作人类型：admin/user/system',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人 ID',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '操作备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_after_sale_id` (`after_sale_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='售后操作日志表';

-- -------------------------------------------
-- 16. 内容投诉表 cms_complaint
-- -------------------------------------------
DROP TABLE IF EXISTS `cms_complaint`;
CREATE TABLE `cms_complaint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '投诉 ID',
    `note_id` BIGINT NOT NULL COMMENT '被投诉笔记 ID',
    `target_type` VARCHAR(20) NOT NULL DEFAULT 'note' COMMENT '投诉对象类型：note笔记，comment评论',
    `comment_id` BIGINT DEFAULT NULL COMMENT '被投诉评论 ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '上一级投诉 ID，用于再次投诉',
    `user_id` BIGINT NOT NULL COMMENT '投诉用户 ID',
    `reason` VARCHAR(50) NOT NULL COMMENT '投诉原因',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '投诉详情',
    `evidence_images` TEXT DEFAULT NULL COMMENT '投诉凭证图片列表（JSON 数组）',
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
    KEY `idx_target` (`target_type`, `note_id`, `comment_id`),
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

-- 演示用户：仅用于展示笔记作者，不提供默认登录账号
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `avatar`, `role`, `status`, `create_time`)
VALUES
    (1, 'petmeet_demo_mali', '$2a$10$XULeTe1Anp4n1uIt65TjgeK9XQpOgUwJhEoSjYaUwt0JbBD.LBCz.', '玛丽', '/images/demo/avatars/mali.jpg', 'user', 1, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (2, 'petmeet_demo_user15', '$2a$10$XULeTe1Anp4n1uIt65TjgeK9XQpOgUwJhEoSjYaUwt0JbBD.LBCz.', '萌宠用户15', '/images/demo/avatars/user-15.jpg', 'user', 1, DATE_SUB(NOW(), INTERVAL 29 DAY)),
    (3, 'petmeet_demo_user05', '$2a$10$XULeTe1Anp4n1uIt65TjgeK9XQpOgUwJhEoSjYaUwt0JbBD.LBCz.', '萌宠用户05', '/images/demo/avatars/user-05.jpg', 'user', 1, DATE_SUB(NOW(), INTERVAL 28 DAY)),
    (4, 'petmeet_demo_user04', '$2a$10$XULeTe1Anp4n1uIt65TjgeK9XQpOgUwJhEoSjYaUwt0JbBD.LBCz.', '萌宠用户04', '/images/demo/avatars/user-04.jpg', 'user', 1, DATE_SUB(NOW(), INTERVAL 27 DAY));

-- 演示商品分类
INSERT INTO `pms_category` (`id`, `name`, `icon`, `sort`, `status`)
VALUES
    (1, '零食', '/category-icons/snack.svg', 10, 1),
    (2, '清洁卫生', '/category-icons/poop-bag.svg', 20, 1),
    (3, '玩具', '/category-icons/frisbee.svg', 30, 1),
    (4, '狗粮', '/category-icons/dog-food.svg', 40, 1),
    (5, '猫粮', '/category-icons/cat-food.svg', 50, 1),
    (6, '服饰', '/category-icons/tshirt.svg', 60, 1);

-- 演示商品：图片均来自本项目原有本地商品图片
INSERT INTO `pms_product`
    (`id`, `category_id`, `name`, `sub_title`, `price`, `stock`, `unit`, `version`, `cover_img`, `cover_imgs`, `detail_imgs`, `description`, `status`, `is_deleted`, `create_time`, `warning_stock`, `sort_weight`, `sales`, `pet_type`, `views`, `related_note_count`)
VALUES
    (1, 3, '长杆羽毛逗猫棒', '钢丝弹簧杆，适合日常互动和消耗精力', 32.00, 199, '支', 0, '/images/demo/products/cat-teaser.jpg', JSON_ARRAY('/images/demo/products/cat-teaser.jpg'), JSON_ARRAY('/images/demo/products/cat-teaser-detail.jpg'), '轻量长杆搭配弹簧结构，可替换逗猫头，适合室内陪玩。', 1, 0, DATE_SUB(NOW(), INTERVAL 7 DAY), 20, 90, 286, 'cat', 2380, 0),
    (2, 6, '猫咪新年加绒背心', '新年喜庆，加绒保暖，轻便好穿', 39.90, 298, '件', 0, '/images/demo/products/cat-new-year-vest.jpg', JSON_ARRAY('/images/demo/products/cat-new-year-vest.jpg'), JSON_ARRAY('/images/demo/products/cat-new-year-vest-detail.jpg'), '适合猫咪冬季居家和短时外出穿着，背心版型活动方便。', 1, 0, DATE_SUB(NOW(), INTERVAL 6 DAY), 20, 85, 168, 'cat', 1860, 0),
    (3, 6, '狗狗新春保暖外套', '加绒保暖，带牵引扣，适合小型犬', 49.90, 300, '件', 0, '/images/demo/products/dog-spring-coat.jpg', JSON_ARRAY('/images/demo/products/dog-spring-coat.jpg'), JSON_ARRAY('/images/demo/products/dog-spring-coat-detail.jpg'), '柔软面料配合保暖内层，适合秋冬季节日常穿着。', 1, 0, DATE_SUB(NOW(), INTERVAL 5 DAY), 20, 80, 152, 'dog', 1710, 0),
    (4, 3, '彩虹猫咪隧道', '可折叠收纳，适合钻洞和追逐互动', 39.90, 198, '个', 0, '/images/demo/products/cat-tunnel.jpg', JSON_ARRAY('/images/demo/products/cat-tunnel.jpg'), JSON_ARRAY('/images/demo/products/cat-tunnel-detail.jpg'), 'S形彩虹隧道，展开后可供猫咪穿梭，闲置时可以折叠收纳。', 1, 0, DATE_SUB(NOW(), INTERVAL 4 DAY), 20, 75, 321, 'cat', 3290, 0),
    (5, 3, '狗狗耐咬拉力圆环', '适合抛接、拉扯和磨牙互动', 29.90, 186, '个', 0, '/images/demo/products/dog-ring.jpg', JSON_ARRAY('/images/demo/products/dog-ring.jpg'), JSON_ARRAY('/images/demo/products/dog-ring-detail.jpg'), '弹性圆环适合主人与狗狗进行抛接和拉扯游戏。', 1, 0, DATE_SUB(NOW(), INTERVAL 3 DAY), 20, 95, 438, 'dog', 4260, 1),
    (6, 1, '冻干牛肝训练零食', '小块分装，适合作为日常训练奖励', 19.90, 200, '袋', 0, '/images/demo/products/beef-liver-snack.webp', JSON_ARRAY('/images/demo/products/beef-liver-snack.webp'), JSON_ARRAY('/images/demo/products/beef-liver-snack-detail.jpg'), '牛肝切片冻干，喂食时应结合宠物体型控制用量。', 1, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), 20, 70, 512, 'general', 5180, 0),
    (7, 2, '宠物环境除味喷雾', '适合猫砂盆和宠物活动区域日常清洁', 23.90, 200, '瓶', 0, '/images/demo/products/pet-deodorizer.jpg', JSON_ARRAY('/images/demo/products/pet-deodorizer.jpg'), JSON_ARRAY('/images/demo/products/pet-deodorizer-detail.jpg'), '用于宠物活动区域的日常清洁和气味管理，使用前请阅读产品说明。', 1, 0, DATE_SUB(NOW(), INTERVAL 1 DAY), 20, 65, 196, 'general', 2030, 0);

-- 演示笔记：图片均来自本项目原有本地笔记图片
INSERT INTO `cms_note`
    (`id`, `user_id`, `title`, `category`, `tags`, `content`, `cover_img`, `images`, `type`, `video_url`, `like_count`, `collect_count`, `status`, `is_deleted`, `is_recommended`, `is_sticky`, `audit_time`, `create_time`)
VALUES
    (1, 1, '宠物玩具终于到了', 'dog', '狗狗玩具,开箱实测,已购反馈', '玩具质量很好，没有异味，圆环适合抛接和拉扯互动。', '/images/demo/notes/toy-arrived.jpg', JSON_ARRAY('/images/demo/notes/toy-arrived.jpg'), 'image', NULL, 28, 12, 1, 0, 1, 1, NOW(), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (2, 2, '小眼神', 'dog', '狗狗生活,可爱,温柔', '它看我的小眼神太有爱了，像在说别担心，有我。', '/images/demo/notes/white-dog.jpg', JSON_ARRAY('/images/demo/notes/white-dog.jpg'), 'image', NULL, 126, 55, 1, 0, 1, 0, NOW(), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (3, 1, '今天仓鼠到了', 'other', '日常,异宠日常,仓鼠', '仓鼠刚到家，目前很活泼，先让它安静熟悉环境。', '/images/demo/notes/hamster.jpg', JSON_ARRAY('/images/demo/notes/hamster.jpg'), 'image', NULL, 88, 31, 1, 0, 1, 0, NOW(), DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    (4, 3, '狗狗剪指甲正确方式', 'knowledge', '科普知识,洗护,指甲,训练', '剪指甲前先安抚情绪，一点点修剪，避免剪到血线。准备止血粉，剪完及时给予奖励。', '/images/demo/notes/nail-guide.jpg', JSON_ARRAY('/images/demo/notes/nail-guide.jpg'), 'image', NULL, 96, 57, 1, 0, 1, 0, NOW(), DATE_SUB(NOW(), INTERVAL 4 HOUR)),
    (5, 4, '猫狗耳朵清洁要点', 'knowledge', '科普知识,耳朵,洗护,健康', '频繁挠耳、异味和分泌物增多都可能是耳道问题信号。不要用棉签深入耳道。', '/images/demo/notes/ear-care.jpg', JSON_ARRAY('/images/demo/notes/ear-care.jpg'), 'image', NULL, 102, 58, 1, 0, 1, 0, NOW(), DATE_SUB(NOW(), INTERVAL 5 HOUR)),
    (6, 3, '为什么要定期驱虫', 'knowledge', '科普知识,驱虫,健康,日常', '体内外寄生虫会影响肠胃和皮肤健康。驱虫周期应结合宠物年龄、体重和兽医建议确定。', '/images/demo/notes/deworm-guide.jpg', JSON_ARRAY('/images/demo/notes/deworm-guide.jpg'), 'image', NULL, 118, 72, 1, 0, 1, 0, NOW(), DATE_SUB(NOW(), INTERVAL 6 HOUR)),
    (7, 1, '快乐的小狗', 'dog', '日常,博美,散步', '今天在草地上玩得很开心，回家后很快就睡着了。', '/images/demo/notes/pomeranian.jpg', JSON_ARRAY('/images/demo/notes/pomeranian.jpg'), 'image', NULL, 143, 46, 1, 0, 0, 0, NOW(), DATE_SUB(NOW(), INTERVAL 7 HOUR)),
    (8, 4, '陪我发呆', 'cat', '猫咪日常,温暖,治愈', '安静陪伴也是猫咪表达信任的一种方式。', '/images/demo/notes/cat-companion.jpg', JSON_ARRAY('/images/demo/notes/cat-companion.jpg'), 'image', NULL, 132, 61, 1, 0, 0, 0, NOW(), DATE_SUB(NOW(), INTERVAL 8 HOUR));

INSERT INTO `cms_note_product_relation` (`note_id`, `product_id`)
VALUES (1, 5);

-- 演示商城横幅和养宠科普卡片
INSERT INTO `cms_banner` (`title`, `position`, `slot`, `image_url`, `keyword`, `link_url`, `link_type`, `sort`, `status`)
VALUES
    ('新年爱宠焕新', 'SHOP_HERO', 'hero', '/images/demo/banners/new-year-dog.jpg', '精选服饰和玩具', '/mall/list?categoryIds=6', 'internal', 1, 1),
    ('猫咪春日穿搭', 'SHOP_HERO', 'hero', '/images/demo/banners/yellow-cat.jpg', '舒适服饰上新', '/mall/list?categoryIds=6', 'internal', 2, 1),
    ('科学换粮法', 'SHOP_TOP', 'card', '/images/demo/science/food-transition.jpg', '换粮', '/community?keyword=换粮', 'internal', 1, 1),
    ('应激反应识别', 'SHOP_TOP', 'card', '/images/demo/science/stress-response.jpg', '应激', '/community?keyword=应激', 'internal', 2, 1),
    ('换季掉毛与毛球症', 'SHOP_TOP', 'card', '/images/demo/science/shedding.jpg', '掉毛', '/community?keyword=掉毛', 'internal', 3, 1),
    ('狗狗洗澡的频率与雷区', 'SHOP_TOP', 'card', '/images/demo/science/dog-bathing.jpg', '洗护', '/community?keyword=洗护', 'internal', 4, 1);

SET FOREIGN_KEY_CHECKS = 1;

-- ==========================================
-- 建表及演示数据初始化完成
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
