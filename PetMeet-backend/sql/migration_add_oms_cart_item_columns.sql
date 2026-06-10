-- 为购物车表补齐逻辑删除与选中字段（可重复执行）
-- 说明：后端实体 OmsCartItem 使用 @TableLogic(isDeleted)；
--      MyBatis-Plus 会在查询中自动拼接 is_deleted=0

-- 1) selected
SELECT IF(
  COUNT(*) = 0,
  'ALTER TABLE `oms_cart_item` ADD COLUMN `selected` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否选中(1选中,0未选中)'' AFTER `quantity`',
  'SELECT 1'
) INTO @sql_selected
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'oms_cart_item'
  AND COLUMN_NAME = 'selected';

PREPARE stmt FROM @sql_selected;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) is_deleted
SELECT IF(
  COUNT(*) = 0,
  'ALTER TABLE `oms_cart_item` ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除：0正常，1删除'' AFTER `selected`',
  'SELECT 1'
) INTO @sql_is_deleted
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'oms_cart_item'
  AND COLUMN_NAME = 'is_deleted';

PREPARE stmt FROM @sql_is_deleted;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
