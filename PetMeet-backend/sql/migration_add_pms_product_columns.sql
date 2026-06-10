-- 在已有表上添加缺失列（与实体类一致）
-- 若某列已存在会报错，可注释掉对应行后重新执行
-- 执行方式: mysql -u root -p < sql\migration_add_pms_product_columns.sql

USE `petmeet`;

-- ---------- pms_product：补充商品表缺失列 ----------
ALTER TABLE `pms_product`
    ADD COLUMN `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位，如：kg、包、个',
    ADD COLUMN `cover_imgs` JSON DEFAULT NULL COMMENT '商品主图组（JSON数组，最多5张）',
    ADD COLUMN `warning_stock` INT DEFAULT NULL COMMENT '预警库存',
    ADD COLUMN `sort_weight` INT DEFAULT NULL COMMENT '排序权重',
    ADD COLUMN `sales` INT NOT NULL DEFAULT 0 COMMENT '销量',
    ADD COLUMN `pet_type` VARCHAR(20) DEFAULT NULL COMMENT '适用宠物：cat/dog/general',
    ADD COLUMN `views` INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    ADD COLUMN `related_note_count` INT NOT NULL DEFAULT 0 COMMENT '关联笔记数';

-- ---------- cms_note：补充笔记表逻辑删除等列 ----------
ALTER TABLE `cms_note`
    ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1删除',
    ADD COLUMN `is_recommended` TINYINT(1) DEFAULT 0 COMMENT '是否推荐',
    ADD COLUMN `is_sticky` TINYINT(1) DEFAULT 0 COMMENT '是否置顶';

-- ---------- oms_order：补充订单表发货与收货人列 ----------
ALTER TABLE `oms_order`
    ADD COLUMN `ship_company` VARCHAR(100) DEFAULT NULL COMMENT '物流公司',
    ADD COLUMN `tracking_no` VARCHAR(100) DEFAULT NULL COMMENT '物流单号',
    ADD COLUMN `ship_time` DATETIME DEFAULT NULL COMMENT '发货时间',
    ADD COLUMN `receiver` VARCHAR(50) DEFAULT NULL COMMENT '收货人姓名',
    ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL COMMENT '收货人电话',
    ADD COLUMN `address` VARCHAR(500) DEFAULT NULL COMMENT '收货地址';
