-- ============================================
-- 修复 pms_product 表缺失字段 (补充版本)
-- ============================================

-- 添加 warning_stock (预警库存)
ALTER TABLE `pms_product` ADD COLUMN `warning_stock` INT DEFAULT 10 COMMENT '预警库存';

-- 添加 sort_weight (排序权重)
ALTER TABLE `pms_product` ADD COLUMN `sort_weight` INT DEFAULT 0 COMMENT '排序权重';

-- 添加 pet_type (适用宠物)
-- 注意: 代码中是 String (cat/dog/general)
ALTER TABLE `pms_product` ADD COLUMN `pet_type` VARCHAR(20) DEFAULT 'general' COMMENT '适用宠物类型';

-- 添加 views (浏览量)
ALTER TABLE `pms_product` ADD COLUMN `views` INT DEFAULT 0 COMMENT '浏览量';

-- 添加 related_note_count (关联笔记数)
ALTER TABLE `pms_product` ADD COLUMN `related_note_count` INT DEFAULT 0 COMMENT '关联笔记数';

-- 添加 unit (单位)
ALTER TABLE `pms_product` ADD COLUMN `unit` VARCHAR(20) DEFAULT '件' COMMENT '单位';

-- 添加 is_deleted (逻辑删除)
ALTER TABLE `pms_product` ADD COLUMN `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0正常,1删除)';

-- 再次尝试添加可能缺失的 oms_order 字段 (为了保险)
ALTER TABLE `oms_order` ADD COLUMN `ship_company` VARCHAR(50) DEFAULT NULL COMMENT '快递公司';
ALTER TABLE `oms_order` ADD COLUMN `tracking_no` VARCHAR(100) DEFAULT NULL COMMENT '快递单号';
ALTER TABLE `oms_order` ADD COLUMN `receiver` VARCHAR(50) DEFAULT NULL COMMENT '收货人姓名';

SELECT 'Product schema fully fixed!' AS message;
