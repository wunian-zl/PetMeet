-- 为售后表补齐凭证图片字段，并优化按订单查询退款的索引（可重复执行）

-- 1) oms_after_sale.evidence_images
SELECT IF(
  COUNT(*) = 0,
  'ALTER TABLE `oms_after_sale` ADD COLUMN `evidence_images` TEXT DEFAULT NULL COMMENT ''退款凭证图片地址列表（JSON 数组）'' AFTER `description`',
  'SELECT 1'
) INTO @sql_add_evidence_images
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'oms_after_sale'
  AND COLUMN_NAME = 'evidence_images';

PREPARE stmt FROM @sql_add_evidence_images;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 为待处理退款查询补齐组合索引（order_id, status）
SELECT IF(
  COUNT(*) = 0,
  'ALTER TABLE `oms_after_sale` ADD INDEX `idx_order_status` (`order_id`, `status`)',
  'SELECT 1'
) INTO @sql_add_idx_order_status
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'oms_after_sale'
  AND INDEX_NAME = 'idx_order_status';

PREPARE stmt FROM @sql_add_idx_order_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
