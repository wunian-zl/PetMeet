-- 支付模块v1：支付流水、退款流水、订单支付字段
SET @db = DATABASE();

CREATE TABLE IF NOT EXISTS `oms_pay_log` (
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
    KEY `idx_trade_no` (`trade_no`),
    KEY `idx_pay_status` (`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付流水表';

CREATE TABLE IF NOT EXISTS `oms_refund_log` (
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

SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_order' AND COLUMN_NAME = 'refund_amount'),
    'SELECT 1',
    'ALTER TABLE `oms_order` ADD COLUMN `refund_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''已退款金额'' AFTER `total_amount`'
) INTO @sql_add_refund_amount;
PREPARE stmt FROM @sql_add_refund_amount; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_order' AND COLUMN_NAME = 'pay_type'),
    'SELECT 1',
    'ALTER TABLE `oms_order` ADD COLUMN `pay_type` TINYINT DEFAULT NULL COMMENT ''支付方式：1支付宝，2微信Mock'' AFTER `status`'
) INTO @sql_add_pay_type;
PREPARE stmt FROM @sql_add_pay_type; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_order' AND COLUMN_NAME = 'pay_sn'),
    'SELECT 1',
    'ALTER TABLE `oms_order` ADD COLUMN `pay_sn` VARCHAR(64) DEFAULT NULL COMMENT ''系统支付流水号'' AFTER `pay_type`'
) INTO @sql_add_pay_sn;
PREPARE stmt FROM @sql_add_pay_sn; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_order' AND COLUMN_NAME = 'trade_no'),
    'SELECT 1',
    'ALTER TABLE `oms_order` ADD COLUMN `trade_no` VARCHAR(128) DEFAULT NULL COMMENT ''第三方交易流水号'' AFTER `pay_sn`'
) INTO @sql_add_trade_no;
PREPARE stmt FROM @sql_add_trade_no; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_order' AND COLUMN_NAME = 'remark'),
    'SELECT 1',
    'ALTER TABLE `oms_order` ADD COLUMN `remark` VARCHAR(500) DEFAULT NULL COMMENT ''订单备注'' AFTER `address`'
) INTO @sql_add_remark;
PREPARE stmt FROM @sql_add_remark; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `oms_order` SET `refund_amount` = 0.00 WHERE `refund_amount` IS NULL;
