-- ============================================
-- 修复 pms_product 表缺失字段
-- ============================================

-- 添加 sales (销量)
ALTER TABLE `pms_product` ADD COLUMN `sales` INT DEFAULT 0 COMMENT '销量';

-- 添加 stock (库存) - 注意: 如果已有stock字段可能会报错, 忽略即可
ALTER TABLE `pms_product` ADD COLUMN `stock` INT DEFAULT 0 COMMENT '库存';

-- 添加 cover_img (封面图) - 如果原表是 cover, 则需要注意
ALTER TABLE `pms_product` ADD COLUMN `cover_img` VARCHAR(255) DEFAULT NULL COMMENT '封面图';

-- 添加 unit (单位)
ALTER TABLE `pms_product` ADD COLUMN `unit` VARCHAR(20) DEFAULT '件' COMMENT '单位';

-- 添加 is_deleted (逻辑删除)
ALTER TABLE `pms_product` ADD COLUMN `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0正常,1删除)';

-- 如果原表有 update_time (代码中曾引用但被我删除了), 这里不添加, 保持代码 entity 一致
-- 代码中 PmsProduct 没有 updateTime

-- 检查 oms_order 是否真的添加成功了
ALTER TABLE `oms_order` ADD COLUMN `ship_company` VARCHAR(50) DEFAULT NULL COMMENT '快递公司';
ALTER TABLE `oms_order` ADD COLUMN `tracking_no` VARCHAR(100) DEFAULT NULL COMMENT '快递单号';
ALTER TABLE `oms_order` ADD COLUMN `receiver` VARCHAR(50) DEFAULT NULL COMMENT '收货人姓名';

SELECT 'Product schema fixed!' AS message;
