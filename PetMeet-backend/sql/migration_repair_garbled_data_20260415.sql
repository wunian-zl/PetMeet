USE `petmeet`;

-- ============================================
-- PetMeet 乱码数据修复脚本（2026-04-15）
-- 这一版只处理已经确认过的数据：
-- 1) 先备份，再清理一批用户已不存在、内容也无法恢复的历史脏数据
-- 2) 修复能够明确判断的错别字
-- 3) 把无法可靠恢复原文的记录单独列出来，留给人工补录
-- ============================================

-- ------------------------------------------------------------
-- 一、先备份本次确认过的脏数据
-- 说明：
-- - 这些备份表只用于回滚和核对，不参与业务查询
-- - 用 INSERT IGNORE 是为了让脚本可以重复执行
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `backup_ums_address_garbled_20260415` LIKE `ums_address`;
INSERT IGNORE INTO `backup_ums_address_garbled_20260415`
SELECT *
FROM `ums_address`
WHERE `id` IN (2, 3, 4, 5, 6, 7, 8, 9, 16);

CREATE TABLE IF NOT EXISTS `backup_oms_order_garbled_20260415` LIKE `oms_order`;
INSERT IGNORE INTO `backup_oms_order_garbled_20260415`
SELECT *
FROM `oms_order`
WHERE `id` IN (7, 9, 10, 11, 12, 13, 14, 15, 16, 17, 24);

CREATE TABLE IF NOT EXISTS `backup_oms_order_item_garbled_20260415` LIKE `oms_order_item`;
INSERT IGNORE INTO `backup_oms_order_item_garbled_20260415`
SELECT *
FROM `oms_order_item`
WHERE `order_id` IN (7, 9, 10, 11, 12, 13, 14, 15, 16, 17, 24);

CREATE TABLE IF NOT EXISTS `backup_oms_after_sale_garbled_20260415` LIKE `oms_after_sale`;
INSERT IGNORE INTO `backup_oms_after_sale_garbled_20260415`
SELECT *
FROM `oms_after_sale`
WHERE `id` IN (1, 2, 3, 4, 5, 6, 11);

CREATE TABLE IF NOT EXISTS `backup_cms_complaint_garbled_20260415` LIKE `cms_complaint`;
INSERT IGNORE INTO `backup_cms_complaint_garbled_20260415`
SELECT *
FROM `cms_complaint`
WHERE `id` IN (4, 7);

CREATE TABLE IF NOT EXISTS `backup_cms_note_garbled_20260415` LIKE `cms_note`;
INSERT IGNORE INTO `backup_cms_note_garbled_20260415`
SELECT *
FROM `cms_note`
WHERE `id` = 175;

-- ------------------------------------------------------------
-- 二、清理确认无效的历史脏数据
-- 这一批数据有两个共同点：
-- 1) 对应用户已经不存在
-- 2) 关键中文字段已经被写成英文问号，原文无法从库里恢复
--
-- 这里按依赖顺序删除：先售后，再订单明细，再订单，最后地址
-- ------------------------------------------------------------

DELETE FROM `oms_after_sale`
WHERE `id` IN (1, 2, 3, 4, 5, 6);

DELETE FROM `oms_order_item`
WHERE `order_id` IN (7, 9, 10, 11, 12, 13, 14, 15, 16, 17, 24);

DELETE FROM `oms_order`
WHERE `id` IN (7, 9, 10, 11, 12, 13, 14, 15, 16, 17, 24);

DELETE FROM `ums_address`
WHERE `id` IN (2, 3, 4, 5, 6, 7, 8, 9, 16);

-- ------------------------------------------------------------
-- 三、修复可以明确判断的错字
-- 这一条不是整段丢失，只是“蓄脃”误写成了“蓄脓”
-- 这里改用十六进制字节写法，避免命令行编码影响更新结果
-- ------------------------------------------------------------

UPDATE `cms_note`
SET
  `title` = CONVERT(0xE5AD90E5AEABE89384E88493E698AFE4BB80E4B988EFBC9FE5A682E4BD95E9A284E998B2 USING utf8mb4),
  `content` = REPLACE(
    `content`,
    CONVERT(0xE5AD90E5AEABE89384E88483 USING utf8mb4),
    CONVERT(0xE5AD90E5AEABE89384E88493 USING utf8mb4)
  )
WHERE `id` = 175;

-- ------------------------------------------------------------
-- 四、列出仍需人工补录的记录
-- 下面这些记录虽然已经备份，但当前库里看不出原始中文，
-- 不能靠猜测直接改值，所以只保留定位清单
-- ------------------------------------------------------------

-- 1) 投诉记录：
--    id=4  的 handle_remark 被写成了 '??'
--    id=7  的 reason / content 整段丢失
SELECT
  `id`,
  `note_id`,
  `user_id`,
  `reason`,
  `content`,
  `handle_remark`,
  `feedback_content`,
  `create_time`
FROM `cms_complaint`
WHERE `id` IN (4, 7)
ORDER BY `id`;

-- 2) 售后记录：
--    id=11 的 reason / description 整段丢失
SELECT
  `id`,
  `order_id`,
  `order_item_id`,
  `user_id`,
  `reason`,
  `description`,
  `handle_remark`,
  `create_time`
FROM `oms_after_sale`
WHERE `id` = 11;

-- ------------------------------------------------------------
-- 五、如果人工确认了原文，可以参考下面的模板手动补录
-- 注意：下面只是模板，先把 xxx 改成确认后的真实内容再执行
-- ------------------------------------------------------------

-- UPDATE `cms_complaint`
-- SET `handle_remark` = 'xxx'
-- WHERE `id` = 4;

-- UPDATE `cms_complaint`
-- SET
--   `reason` = 'xxx',
--   `content` = 'xxx'
-- WHERE `id` = 7;

-- UPDATE `oms_after_sale`
-- SET
--   `reason` = 'xxx',
--   `description` = 'xxx'
-- WHERE `id` = 11;

-- ------------------------------------------------------------
-- 六、执行后建议复查的 SQL
-- ------------------------------------------------------------

SELECT 'ums_address' AS `table_name`, COUNT(*) AS `bad_rows`
FROM `ums_address`
WHERE `name` REGEXP '[?]{2,}'
   OR `province` REGEXP '[?]{2,}'
   OR `city` REGEXP '[?]{2,}'
   OR `region` REGEXP '[?]{2,}'
   OR `detail_address` REGEXP '[?]{2,}'

UNION ALL

SELECT 'oms_order' AS `table_name`, COUNT(*) AS `bad_rows`
FROM `oms_order`
WHERE `receiver` REGEXP '[?]{2,}'
   OR `address` REGEXP '[?]{2,}'
   OR CAST(`receiver_info` AS CHAR) REGEXP '[?]{2,}'

UNION ALL

SELECT 'cms_complaint' AS `table_name`, COUNT(*) AS `bad_rows`
FROM `cms_complaint`
WHERE `reason` REGEXP '[?]{2,}'
   OR `content` REGEXP '[?]{2,}'
   OR `handle_remark` REGEXP '[?]{2,}'
   OR `feedback_content` REGEXP '[?]{2,}'

UNION ALL

SELECT 'oms_after_sale' AS `table_name`, COUNT(*) AS `bad_rows`
FROM `oms_after_sale`
WHERE `reason` REGEXP '[?]{2,}'
   OR `description` REGEXP '[?]{2,}'
   OR `handle_remark` REGEXP '[?]{2,}'
   OR `evidence_images` REGEXP '[?]{2,}';
