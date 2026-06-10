-- 为订单补齐评价字段，并新增售后申请表（可重复执行，需 MySQL 8+）

SET @db_name := DATABASE();

-- 1) oms_order.review_status
SELECT IF(
  COUNT(*) = 0,
  'ALTER TABLE `oms_order` ADD COLUMN `review_status` TINYINT NOT NULL DEFAULT 0 COMMENT ''评价状态：0待评价，1已评价'' AFTER `status`',
  'SELECT 1'
) INTO @sql_review_status
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'oms_order'
  AND COLUMN_NAME = 'review_status';
PREPARE stmt FROM @sql_review_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) oms_order.review_score
SELECT IF(
  COUNT(*) = 0,
  'ALTER TABLE `oms_order` ADD COLUMN `review_score` TINYINT DEFAULT NULL COMMENT ''评价分数（1-5）'' AFTER `review_status`',
  'SELECT 1'
) INTO @sql_review_score
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'oms_order'
  AND COLUMN_NAME = 'review_score';
PREPARE stmt FROM @sql_review_score;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) oms_order.review_content
SELECT IF(
  COUNT(*) = 0,
  'ALTER TABLE `oms_order` ADD COLUMN `review_content` VARCHAR(500) DEFAULT NULL COMMENT ''评价内容'' AFTER `review_score`',
  'SELECT 1'
) INTO @sql_review_content
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'oms_order'
  AND COLUMN_NAME = 'review_content';
PREPARE stmt FROM @sql_review_content;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) oms_order.review_time
SELECT IF(
  COUNT(*) = 0,
  'ALTER TABLE `oms_order` ADD COLUMN `review_time` DATETIME DEFAULT NULL COMMENT ''评价时间'' AFTER `review_content`',
  'SELECT 1'
) INTO @sql_review_time
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'oms_order'
  AND COLUMN_NAME = 'review_time';
PREPARE stmt FROM @sql_review_time;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5) oms_after_sale 表（新环境直接创建）
CREATE TABLE IF NOT EXISTS `oms_after_sale` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '售后申请 ID',
  `order_id` BIGINT NOT NULL COMMENT '订单 ID',
  `order_item_id` BIGINT NOT NULL COMMENT '订单项 ID',
  `user_id` BIGINT NOT NULL COMMENT '申请用户 ID',
  `type` TINYINT NOT NULL DEFAULT 0 COMMENT '售后类型：0仅退款，1退货退款，2换货',
  `reason` VARCHAR(100) DEFAULT NULL COMMENT '申请原因',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '问题说明',
  `evidence_images` TEXT DEFAULT NULL COMMENT '退款凭证图片地址列表（JSON 数组）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0待处理，1处理中，2已完成，3已拒绝，4已取消',
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

-- 6) 老环境补齐 oms_after_sale.evidence_images 字段
SELECT IF(
  COUNT(*) = 0,
  'ALTER TABLE `oms_after_sale` ADD COLUMN `evidence_images` TEXT DEFAULT NULL COMMENT ''退款凭证图片地址列表（JSON 数组）'' AFTER `description`',
  'SELECT 1'
) INTO @sql_add_evidence_images
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'oms_after_sale'
  AND COLUMN_NAME = 'evidence_images';
PREPARE stmt FROM @sql_add_evidence_images;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 7) 老环境补齐 idx_order_status 索引
SELECT IF(
  COUNT(*) = 0,
  'ALTER TABLE `oms_after_sale` ADD INDEX `idx_order_status` (`order_id`, `status`)',
  'SELECT 1'
) INTO @sql_add_idx_order_status
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'oms_after_sale'
  AND INDEX_NAME = 'idx_order_status';
PREPARE stmt FROM @sql_add_idx_order_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
