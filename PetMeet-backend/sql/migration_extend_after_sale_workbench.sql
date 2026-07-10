-- 售后工作台：扩展售后字段、状态承载字段和操作日志表（可重复执行，需 MySQL 8+）

SET @db := DATABASE();

SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_after_sale' AND COLUMN_NAME = 'refund_amount'),
  'SELECT 1',
  'ALTER TABLE `oms_after_sale` ADD COLUMN `refund_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''售后退款金额'' AFTER `evidence_images`'
) INTO @sql;
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_after_sale' AND COLUMN_NAME = 'return_address'),
  'SELECT 1',
  'ALTER TABLE `oms_after_sale` ADD COLUMN `return_address` VARCHAR(500) DEFAULT NULL COMMENT ''商家退货地址'' AFTER `refund_amount`'
) INTO @sql;
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_after_sale' AND COLUMN_NAME = 'return_company'),
  'SELECT 1',
  'ALTER TABLE `oms_after_sale` ADD COLUMN `return_company` VARCHAR(100) DEFAULT NULL COMMENT ''买家退货物流公司'' AFTER `return_address`'
) INTO @sql;
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_after_sale' AND COLUMN_NAME = 'return_tracking_no'),
  'SELECT 1',
  'ALTER TABLE `oms_after_sale` ADD COLUMN `return_tracking_no` VARCHAR(100) DEFAULT NULL COMMENT ''买家退货物流单号'' AFTER `return_company`'
) INTO @sql;
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_after_sale' AND COLUMN_NAME = 'return_ship_time'),
  'SELECT 1',
  'ALTER TABLE `oms_after_sale` ADD COLUMN `return_ship_time` DATETIME DEFAULT NULL COMMENT ''买家退货发出时间'' AFTER `return_tracking_no`'
) INTO @sql;
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_after_sale' AND COLUMN_NAME = 'return_receive_time'),
  'SELECT 1',
  'ALTER TABLE `oms_after_sale` ADD COLUMN `return_receive_time` DATETIME DEFAULT NULL COMMENT ''商家确认收货时间'' AFTER `return_ship_time`'
) INTO @sql;
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_after_sale' AND COLUMN_NAME = 'exchange_company'),
  'SELECT 1',
  'ALTER TABLE `oms_after_sale` ADD COLUMN `exchange_company` VARCHAR(100) DEFAULT NULL COMMENT ''换货物流公司'' AFTER `return_receive_time`'
) INTO @sql;
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_after_sale' AND COLUMN_NAME = 'exchange_tracking_no'),
  'SELECT 1',
  'ALTER TABLE `oms_after_sale` ADD COLUMN `exchange_tracking_no` VARCHAR(100) DEFAULT NULL COMMENT ''换货物流单号'' AFTER `exchange_company`'
) INTO @sql;
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_after_sale' AND COLUMN_NAME = 'exchange_ship_time'),
  'SELECT 1',
  'ALTER TABLE `oms_after_sale` ADD COLUMN `exchange_ship_time` DATETIME DEFAULT NULL COMMENT ''换货发货时间'' AFTER `exchange_tracking_no`'
) INTO @sql;
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `oms_after_sale` SET `refund_amount` = 0.00 WHERE `refund_amount` IS NULL;

CREATE TABLE IF NOT EXISTS `oms_after_sale_log` (
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

SELECT IF(
  EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oms_after_sale' AND INDEX_NAME = 'idx_type_status'),
  'SELECT 1',
  'ALTER TABLE `oms_after_sale` ADD INDEX `idx_type_status` (`type`, `status`)'
) INTO @sql;
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
