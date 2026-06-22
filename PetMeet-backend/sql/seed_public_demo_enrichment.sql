-- PetMeet线上演示数据增强脚本
-- 用途：补充笔记、评论、互动、订单、售后等演示数据，并修复部分图文错配记录。
-- 安全边界：不DROP业务表；仅清理用户名以pm_seed_开头、订单号以PMD开头的本脚本演示数据。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
USE `petmeet`;

START TRANSACTION;

-- 1.清理旧版脚本演示数据，保证重复执行不会产生重复记录。
DROP TEMPORARY TABLE IF EXISTS tmp_pm_seed_user_ids;
CREATE TEMPORARY TABLE tmp_pm_seed_user_ids AS
SELECT `id` FROM `sys_user` WHERE `username` LIKE 'pm_seed_%';

DROP TEMPORARY TABLE IF EXISTS tmp_pm_seed_note_ids;
CREATE TEMPORARY TABLE tmp_pm_seed_note_ids AS
SELECT `id` FROM `cms_note` WHERE `user_id` IN (SELECT `id` FROM tmp_pm_seed_user_ids);

DROP TEMPORARY TABLE IF EXISTS tmp_pm_seed_order_ids;
CREATE TEMPORARY TABLE tmp_pm_seed_order_ids AS
SELECT `id` FROM `oms_order`
WHERE `order_sn` LIKE 'PMD20260621%' OR `user_id` IN (SELECT `id` FROM tmp_pm_seed_user_ids);

DROP TEMPORARY TABLE IF EXISTS tmp_pm_seed_comment_ids;
CREATE TEMPORARY TABLE tmp_pm_seed_comment_ids AS
SELECT `id` FROM `cms_comment`
WHERE `user_id` IN (SELECT `id` FROM tmp_pm_seed_user_ids)
   OR `note_id` IN (SELECT `id` FROM tmp_pm_seed_note_ids);

DELETE FROM `sys_interaction`
WHERE `user_id` IN (SELECT `id` FROM tmp_pm_seed_user_ids)
   OR (`type` IN (1,2) AND `target_id` IN (SELECT `id` FROM tmp_pm_seed_note_ids))
   OR (`type` = 3 AND `target_id` IN (SELECT `id` FROM tmp_pm_seed_comment_ids));

DELETE FROM `cms_complaint`
WHERE `user_id` IN (SELECT `id` FROM tmp_pm_seed_user_ids)
   OR `note_id` IN (SELECT `id` FROM tmp_pm_seed_note_ids)
   OR `comment_id` IN (SELECT `id` FROM tmp_pm_seed_comment_ids);

DELETE FROM `cms_comment`
WHERE `reply_to_id` IN (SELECT `id` FROM tmp_pm_seed_comment_ids);
DELETE FROM `cms_comment`
WHERE `parent_id` IN (SELECT `id` FROM tmp_pm_seed_comment_ids);
DELETE FROM `cms_comment`
WHERE `note_id` IN (SELECT `id` FROM tmp_pm_seed_note_ids);
DELETE FROM `cms_comment`
WHERE `user_id` IN (SELECT `id` FROM tmp_pm_seed_user_ids);

DELETE FROM `cms_note_product_relation`
WHERE `note_id` IN (SELECT `id` FROM tmp_pm_seed_note_ids);

DELETE FROM `sys_notification`
WHERE `user_id` IN (SELECT `id` FROM tmp_pm_seed_user_ids)
   OR (`biz_type` IN ('note','comment') AND `biz_id` IN (SELECT `id` FROM tmp_pm_seed_note_ids))
   OR (`biz_type` LIKE 'order%' AND `biz_id` IN (SELECT `id` FROM tmp_pm_seed_order_ids));

DELETE FROM `oms_refund_log` WHERE `order_id` IN (SELECT `id` FROM tmp_pm_seed_order_ids);
DELETE FROM `oms_after_sale` WHERE `order_id` IN (SELECT `id` FROM tmp_pm_seed_order_ids);
DELETE FROM `oms_pay_log` WHERE `order_id` IN (SELECT `id` FROM tmp_pm_seed_order_ids);
DELETE FROM `oms_order_item` WHERE `order_id` IN (SELECT `id` FROM tmp_pm_seed_order_ids);
DELETE FROM `oms_order` WHERE `id` IN (SELECT `id` FROM tmp_pm_seed_order_ids);
DELETE FROM `ums_address` WHERE `user_id` IN (SELECT `id` FROM tmp_pm_seed_user_ids);
DELETE FROM `sys_follow`
WHERE `follower_id` IN (SELECT `id` FROM tmp_pm_seed_user_ids);
DELETE FROM `sys_follow`
WHERE `followee_id` IN (SELECT `id` FROM tmp_pm_seed_user_ids);
DELETE FROM `cms_note` WHERE `id` IN (SELECT `id` FROM tmp_pm_seed_note_ids);
DELETE FROM `sys_user` WHERE `id` IN (SELECT `id` FROM tmp_pm_seed_user_ids);

-- 2.修复当前线上已知图文错配，图片均来自uploads/demo。
UPDATE `cms_note`
SET `cover_img`='/images/demo/notes/cat-companion.jpg',
    `images`=JSON_ARRAY('/images/demo/notes/cat-companion.jpg'),
    `category`='cat',
    `tags`='猫咪日常,新手养猫,适应期'
WHERE `title`='新猫到家第一周记录';

UPDATE `cms_note`
SET `cover_img`='/images/demo/notes-extra/note-extra-06.jpg',
    `images`=JSON_ARRAY('/images/demo/notes-extra/note-extra-06.jpg'),
    `category`='cat'
WHERE `title`='猫咪喝水观察记';

UPDATE `cms_note`
SET `cover_img`='/images/demo/notes-extra/note-extra-02.jpg',
    `images`=JSON_ARRAY('/images/demo/notes-extra/note-extra-02.jpg'),
    `category`='dog'
WHERE `title`='雨天遛狗小经验';

UPDATE `cms_note`
SET `cover_img`='/images/demo/notes/pomeranian.jpg',
    `images`=JSON_ARRAY('/images/demo/notes/pomeranian.jpg'),
    `category`='dog'
WHERE `title` IN ('小型犬冬天出门护理','博美散步后的护理');

UPDATE `cms_note`
SET `cover_img`='/images/demo/notes/hamster.jpg',
    `images`=JSON_ARRAY('/images/demo/notes/hamster.jpg'),
    `category`='other',
    `tags`='异宠日常,仓鼠,观察记录'
WHERE `title` IN ('仓鼠夜间活动观察','新手养仓鼠避坑');

UPDATE `cms_note`
SET `cover_img`='/images/demo/notes-extra/note-extra-07.jpg',
    `images`=JSON_ARRAY('/images/demo/notes-extra/note-extra-07.jpg'),
    `category`='cat'
WHERE `title`='猫咪钻纸箱的一天';

UPDATE `cms_note`
SET `cover_img`='/images/demo/notes-extra/note-extra-03.jpg',
    `images`=JSON_ARRAY('/images/demo/notes-extra/note-extra-03.jpg'),
    `category`='dog'
WHERE `title`='狗狗磨牙期怎么陪玩';

UPDATE `cms_note`
SET `cover_img`='/images/demo/notes/ear-care.jpg',
    `images`=JSON_ARRAY('/images/demo/notes/ear-care.jpg'),
    `category`='knowledge',
    `tags`='科普知识,耳朵,洗护,健康'
WHERE `title` IN ('猫狗耳朵清洁要点','猫狗耳朵清洁复盘');

UPDATE `cms_note`
SET `cover_img`='/images/demo/science/food-transition.jpg',
    `images`=JSON_ARRAY('/images/demo/science/food-transition.jpg'),
    `category`='knowledge'
WHERE `title` IN ('换粮第7天记录','换粮第七天记录');

UPDATE `cms_note`
SET `cover_img`='/images/demo/science/dog-bathing.jpg',
    `images`=JSON_ARRAY('/images/demo/science/dog-bathing.jpg'),
    `category`='knowledge'
WHERE `title`='宠物生活区清洁记录';

UPDATE `cms_note`
SET `cover_img`='/images/demo/notes-extra/note-extra-10.jpg',
    `images`=JSON_ARRAY('/images/demo/notes-extra/note-extra-10.jpg'),
    `category`='cat'
WHERE `title`='布置一个安静的猫窝角';

UPDATE `cms_note`
SET `cover_img`='/images/demo/science/shedding.jpg',
    `images`=JSON_ARRAY('/images/demo/science/shedding.jpg'),
    `category`='cat',
    `tags`='掉毛,梳毛,猫咪健康'
WHERE `title`='猫咪掉毛季梳毛记录';

UPDATE `cms_note`
SET `cover_img`='/images/demo/notes-extra/note-extra-02.jpg',
    `images`=JSON_ARRAY('/images/demo/notes-extra/note-extra-02.jpg'),
    `category`='dog'
WHERE `title`='狗狗训练奖励节奏记录';

UPDATE `cms_note`
SET `cover_img`='/images/demo/notes/cat-companion.jpg',
    `images`=JSON_ARRAY('/images/demo/notes/cat-companion.jpg','/images/demo/notes/pomeranian.jpg')
WHERE `title`='陪伴型宠物日常随拍';

-- 3.修复旧补充商品的“标题和图片不一致”问题，只改旧脚本生成的错配商品。
UPDATE `pms_product`
SET `name`='猫咪彩虹隧道加厚款',
    `sub_title`='可折叠收纳，适合钻洞、追逐和午休',
    `description`='彩虹隧道展开后空间充足，适合猫咪日常穿梭和互动玩耍。'
WHERE `name`='猫咪磨爪瓦楞板' AND `cover_img`='/images/demo/products/cat-tunnel.jpg';

UPDATE `pms_product`
SET `name`='宠物环境除味喷雾便携装',
    `sub_title`='适合猫砂盆和宠物活动区域日常清洁',
    `description`='用于宠物活动区域的气味管理，清洁后保持通风干燥。'
WHERE `name`='便携宠物拾便袋套装' AND `cover_img`='/images/demo/products/pet-deodorizer.jpg';

UPDATE `pms_product`
SET `name`='狗狗耐咬训练圆环加厚款',
    `sub_title`='适合抛接、拉扯和消耗精力',
    `description`='弹性圆环适合主人和狗狗进行互动训练，日常玩耍后建议清洁晾干。'
WHERE `name`='狗狗训练响片' AND `cover_img`='/images/demo/products/dog-ring.jpg';

UPDATE `pms_product`
SET `name`='长杆羽毛逗猫棒替换装',
    `sub_title`='适合日常互动和消耗精力',
    `description`='轻量长杆搭配羽毛逗猫头，适合室内陪玩和训练猫咪反应。'
WHERE `name`='猫咪换季梳毛刷' AND `cover_img`='/images/demo/products/cat-teaser.jpg';

UPDATE `pms_product`
SET `name`='狗狗春季轻薄外套',
    `sub_title`='轻薄保暖，适合春秋短时外出',
    `description`='柔软面料配合轻薄版型，适合小型犬短时外出和节日拍照。'
WHERE `name`='宠物外出折叠水碗' AND `cover_img`='/images/demo/products/dog-spring-coat.jpg';

-- 4.新增演示用户。
INSERT INTO `sys_user`
(`username`,`password`,`nickname`,`avatar`,`role`,`phone`,`email`,`gender`,`tags`,`status`,`create_time`,`last_login_time`)
VALUES
('pm_seed_001','$2a$10$XULeTe1Anp4n1uIt65TjgeK9XQpOgUwJhEoSjYaUwt0JbBD.LBCz.','桃桃和柯基','/images/demo/avatars/user-15.jpg','user','13800061001','pm_seed_001@example.com','female','柯基,遛狗,玩具',1,DATE_SUB(NOW(),INTERVAL 45 DAY),DATE_SUB(NOW(),INTERVAL 2 HOUR)),
('pm_seed_002','$2a$10$XULeTe1Anp4n1uIt65TjgeK9XQpOgUwJhEoSjYaUwt0JbBD.LBCz.','小满的猫窝','/images/demo/avatars/mali.jpg','user','13800061002','pm_seed_002@example.com','female','英短,猫咪日常,清洁',1,DATE_SUB(NOW(),INTERVAL 42 DAY),DATE_SUB(NOW(),INTERVAL 3 HOUR)),
('pm_seed_003','$2a$10$XULeTe1Anp4n1uIt65TjgeK9XQpOgUwJhEoSjYaUwt0JbBD.LBCz.','白糖养宠记','/images/demo/avatars/user-05.jpg','user','13800061003','pm_seed_003@example.com','male','小型犬,洗护,训练',1,DATE_SUB(NOW(),INTERVAL 40 DAY),DATE_SUB(NOW(),INTERVAL 5 HOUR)),
('pm_seed_004','$2a$10$XULeTe1Anp4n1uIt65TjgeK9XQpOgUwJhEoSjYaUwt0JbBD.LBCz.','阿布的阳台','/images/demo/avatars/user-04.jpg','user','13800061004','pm_seed_004@example.com','other','长毛猫,掉毛,晒太阳',1,DATE_SUB(NOW(),INTERVAL 38 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY)),
('pm_seed_005','$2a$10$XULeTe1Anp4n1uIt65TjgeK9XQpOgUwJhEoSjYaUwt0JbBD.LBCz.','仓鼠观察员','/images/demo/avatars/user-15.jpg','user','13800061005','pm_seed_005@example.com','female','仓鼠,异宠,观察',1,DATE_SUB(NOW(),INTERVAL 34 DAY),DATE_SUB(NOW(),INTERVAL 4 HOUR)),
('pm_seed_006','$2a$10$XULeTe1Anp4n1uIt65TjgeK9XQpOgUwJhEoSjYaUwt0JbBD.LBCz.','豚鼠慢生活','/images/demo/avatars/mali.jpg','user','13800061006','pm_seed_006@example.com','male','豚鼠,草地,陪伴',1,DATE_SUB(NOW(),INTERVAL 31 DAY),DATE_SUB(NOW(),INTERVAL 7 HOUR)),
('pm_seed_007','$2a$10$XULeTe1Anp4n1uIt65TjgeK9XQpOgUwJhEoSjYaUwt0JbBD.LBCz.','鹦鹉碎碎念','/images/demo/avatars/user-05.jpg','user','13800061007','pm_seed_007@example.com','female','鹦鹉,鸟类,互动',1,DATE_SUB(NOW(),INTERVAL 29 DAY),DATE_SUB(NOW(),INTERVAL 8 HOUR)),
('pm_seed_008','$2a$10$XULeTe1Anp4n1uIt65TjgeK9XQpOgUwJhEoSjYaUwt0JbBD.LBCz.','爬宠日记本','/images/demo/avatars/user-04.jpg','user','13800061008','pm_seed_008@example.com','male','鬃狮蜥,爬宠,晒背',1,DATE_SUB(NOW(),INTERVAL 25 DAY),DATE_SUB(NOW(),INTERVAL 9 HOUR)),
('pm_seed_009','$2a$10$XULeTe1Anp4n1uIt65TjgeK9XQpOgUwJhEoSjYaUwt0JbBD.LBCz.','猫狗双拼家','/images/demo/avatars/user-15.jpg','user','13800061009','pm_seed_009@example.com','female','猫狗双全,好物,订单',1,DATE_SUB(NOW(),INTERVAL 21 DAY),DATE_SUB(NOW(),INTERVAL 6 HOUR)),
('pm_seed_010','$2a$10$XULeTe1Anp4n1uIt65TjgeK9XQpOgUwJhEoSjYaUwt0JbBD.LBCz.','养宠笔记夹','/images/demo/avatars/mali.jpg','user','13800061010','pm_seed_010@example.com','other','科普,收藏,复盘',1,DATE_SUB(NOW(),INTERVAL 18 DAY),DATE_SUB(NOW(),INTERVAL 10 HOUR));

INSERT INTO `ums_address`
(`user_id`,`name`,`phone`,`province`,`city`,`region`,`detail_address`,`is_default`)
SELECT `id`,CASE `username`
    WHEN 'pm_seed_001' THEN '李桃'
    WHEN 'pm_seed_002' THEN '陈满'
    WHEN 'pm_seed_003' THEN '周白'
    WHEN 'pm_seed_004' THEN '吴阿布'
    WHEN 'pm_seed_005' THEN '赵圆'
    WHEN 'pm_seed_006' THEN '钱禾'
    WHEN 'pm_seed_007' THEN '孙青'
    WHEN 'pm_seed_008' THEN '郑岩'
    WHEN 'pm_seed_009' THEN '王双'
    ELSE '林记'
END,
`phone`,'广东省','深圳市','南山区',
CASE `username`
    WHEN 'pm_seed_001' THEN '海德三道宠友公寓3栋1201'
    WHEN 'pm_seed_002' THEN '科技园南区猫窝花园2栋602'
    WHEN 'pm_seed_003' THEN '后海大道白糖小区8栋1105'
    WHEN 'pm_seed_004' THEN '粤海街道阳台里1栋1808'
    WHEN 'pm_seed_005' THEN '南山大道观察员公寓5栋905'
    WHEN 'pm_seed_006' THEN '蛇口湾慢生活花园6栋706'
    WHEN 'pm_seed_007' THEN '深圳湾花园鸟鸣苑9栋303'
    WHEN 'pm_seed_008' THEN '前海路爬宠公寓7栋1502'
    WHEN 'pm_seed_009' THEN '高新南十道双拼家园10栋808'
    ELSE '南头街道笔记夹小区4栋401'
END,
1
FROM `sys_user`
WHERE `username` LIKE 'pm_seed_%';

INSERT IGNORE INTO `sys_follow`(`follower_id`,`followee_id`,`create_time`)
SELECT a.`id`,b.`id`,DATE_SUB(NOW(),INTERVAL (a.`id`+b.`id`) HOUR)
FROM `sys_user` a
JOIN `sys_user` b ON a.`id`<>b.`id`
WHERE a.`username` LIKE 'pm_seed_%'
  AND b.`username` LIKE 'pm_seed_%'
  AND MOD(a.`id`+b.`id`,4)=0;

-- 5.新增图文一致的笔记。
DROP TEMPORARY TABLE IF EXISTS tmp_pm_seed_notes;
CREATE TEMPORARY TABLE tmp_pm_seed_notes(
  `username` VARCHAR(50) NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `category` VARCHAR(50) NOT NULL,
  `tags` VARCHAR(255) NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `cover_img` VARCHAR(500) NOT NULL,
  `images` JSON NOT NULL,
  `like_count` INT NOT NULL,
  `collect_count` INT NOT NULL,
  `is_recommended` TINYINT NOT NULL,
  `is_sticky` TINYINT NOT NULL,
  `created_hours_ago` INT NOT NULL,
  `product_name` VARCHAR(200) DEFAULT NULL
);

INSERT INTO tmp_pm_seed_notes
(`username`,`title`,`category`,`tags`,`content`,`cover_img`,`images`,`like_count`,`collect_count`,`is_recommended`,`is_sticky`,`created_hours_ago`,`product_name`)
VALUES
('pm_seed_001','柯基坐姿太会营业了','dog','狗狗生活,柯基,日常记录','今天只是想拍一张坐姿，结果它自己找好了角度，眼神也很配合。短腿小狗的快乐很直接，记录一下。','/images/demo/notes-extra/note-extra-01.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-01.jpg'),64,18,1,1,1,NULL),
('pm_seed_003','草地上的狗狗聚会','dog','狗狗生活,社交,户外','周末在草地碰到几只性格很好的狗狗，大家先闻闻再一起跑，整个下午都很放松。','/images/demo/notes-extra/note-extra-02.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-02.jpg'),58,20,1,0,2,'狗狗耐咬拉力圆环'),
('pm_seed_001','柴犬镜头感满分','dog','狗狗生活,柴犬,拍照','柴犬今天特别愿意看镜头，拍照前先让它坐稳，再用零食引导，出片率明显高很多。','/images/demo/notes-extra/note-extra-03.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-03.jpg'),71,25,1,0,3,'冻干牛肝训练零食'),
('pm_seed_003','白色小狗出门前的开心脸','dog','狗狗生活,出门,陪伴','牵引绳一拿出来就开始摇尾巴，出门前先检查脚垫和饮水，短途散步也要照顾节奏。','/images/demo/notes-extra/note-extra-04.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-04.jpg'),83,31,0,0,4,'狗狗春季轻薄外套'),
('pm_seed_009','边牧草地梳毛记录','dog','狗狗生活,边牧,梳毛','户外活动回来先简单梳一遍毛，再看皮肤有没有草籽和小结，长毛犬真的需要耐心。','/images/demo/notes-extra/note-extra-05.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-05.jpg'),77,26,0,0,5,NULL),
('pm_seed_002','英短今天很会摆拍','cat','猫咪日常,英短,拍照','窗边光线刚好，英短站在门口看了我好几秒，表情像是在等我按快门。','/images/demo/notes-extra/note-extra-06.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-06.jpg'),92,42,1,0,6,NULL),
('pm_seed_004','长毛猫午后休息','cat','猫咪日常,长毛猫,午睡','午后的长毛猫很适合观察呼吸和精神状态，睡醒后再梳毛，配合度会高很多。','/images/demo/notes-extra/note-extra-07.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-07.jpg'),88,33,1,0,7,NULL),
('pm_seed_002','缅因猫眼神太有故事','cat','猫咪日常,缅因,陪伴','缅因猫的表情很稳，靠近拍照前先让它闻一下镜头，整个过程会更轻松。','/images/demo/notes-extra/note-extra-08.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-08.jpg'),81,29,0,0,8,NULL),
('pm_seed_010','银渐层午睡观察','cat','猫咪日常,银渐层,午睡','午睡前后精神状态都不错，最近饮水也稳定。每天简单记录一次，比临时回忆靠谱。','/images/demo/notes-extra/note-extra-09.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-09.jpg'),69,24,0,0,9,NULL),
('pm_seed_004','暹罗猫的床边巡逻','cat','猫咪日常,暹罗,居家','它每天都会绕床边巡逻一圈，确认环境没变化后才安心躺下，安全感真的很重要。','/images/demo/notes-extra/note-extra-10.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-10.jpg'),75,28,0,0,10,NULL),
('pm_seed_005','仓鼠吃饭慢镜头','other','异宠日常,仓鼠,进食观察','仓鼠吃东西的动作很快，拍慢一点才能看清。新食物先少量尝试，观察便便和精神状态。','/images/demo/notes-extra/note-extra-11.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-11.jpg'),56,19,1,0,11,NULL),
('pm_seed_005','龙猫新垫料适应记','other','异宠日常,龙猫,垫料','换垫料后先观察有没有频繁抓挠和打喷嚏，笼内保持干燥比什么都重要。','/images/demo/notes-extra/note-extra-12.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-12.jpg'),47,17,0,0,12,NULL),
('pm_seed_006','豚鼠户外晒太阳','other','异宠日常,豚鼠,户外','豚鼠短时间晒太阳很治愈，但一定要注意温度、遮阴和饮水，不能离开视线。','/images/demo/notes-extra/note-extra-13.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-13.jpg'),63,23,0,0,13,NULL),
('pm_seed_007','虎皮鹦鹉互相理羽','other','异宠日常,鸟类,鹦鹉','两只虎皮鹦鹉互相理羽时很安静，说明状态比较放松。笼舍清洁和作息稳定很关键。','/images/demo/notes-extra/note-extra-14.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-14.jpg'),79,30,1,0,14,NULL),
('pm_seed_007','玄凤鹦鹉贴贴时刻','other','异宠日常,鸟类,玄凤','玄凤今天愿意靠近镜头，羽冠也很放松。互动时间不长，但质量很高。','/images/demo/notes-extra/note-extra-15.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-15.jpg'),73,27,0,0,15,NULL),
('pm_seed_008','鬃狮蜥晒背记录','other','异宠日常,鬃狮蜥,爬宠','晒背点温度要稳定，拍照时不要频繁移动它。状态好不好，看精神和进食最直接。','/images/demo/notes-extra/note-extra-16.jpg',JSON_ARRAY('/images/demo/notes-extra/note-extra-16.jpg'),68,21,0,0,16,NULL),
('pm_seed_002','新猫到家先别急着抱','cat','猫咪日常,新手养猫,适应期','新猫到家第一天先给安静角落，水、粮、猫砂盆放近一点。愿意出来探索时再轻声互动。','/images/demo/notes/cat-companion.jpg',JSON_ARRAY('/images/demo/notes/cat-companion.jpg'),97,44,1,0,17,NULL),
('pm_seed_010','驱虫提醒别只靠记忆','knowledge','科普知识,驱虫,健康','体内外驱虫周期要结合年龄、体重和外出频率决定，建议把日期写进提醒里，避免漏掉。','/images/demo/notes/deworm-guide.jpg',JSON_ARRAY('/images/demo/notes/deworm-guide.jpg'),89,53,1,0,18,NULL),
('pm_seed_010','猫狗耳朵清洁复习','knowledge','科普知识,耳朵,洗护','耳朵有异味、分泌物变多或者频繁抓挠时要提高警惕。日常清洁只处理外耳廓，不要深入耳道。','/images/demo/notes/ear-care.jpg',JSON_ARRAY('/images/demo/notes/ear-care.jpg'),94,49,1,0,19,NULL),
('pm_seed_005','仓鼠换笼别太急','other','异宠日常,仓鼠,换笼','新笼子先放一点旧垫料，让气味过渡自然。刚换环境时减少上手频率，观察吃喝更重要。','/images/demo/notes/hamster.jpg',JSON_ARRAY('/images/demo/notes/hamster.jpg'),61,22,0,0,20,NULL),
('pm_seed_003','指甲修剪前先脱敏','knowledge','科普知识,指甲,训练','先让狗狗习惯摸脚，再逐步接触指甲剪。一次不用剪太多，完成后及时奖励。','/images/demo/notes/nail-guide.jpg',JSON_ARRAY('/images/demo/notes/nail-guide.jpg'),86,46,1,0,21,'冻干牛肝训练零食'),
('pm_seed_003','博美牵引散步训练','dog','狗狗生活,博美,牵引','博美出门容易兴奋，先在楼下短距离练习跟随，稳定后再延长路线。','/images/demo/notes/pomeranian.jpg',JSON_ARRAY('/images/demo/notes/pomeranian.jpg'),78,34,0,0,22,NULL),
('pm_seed_009','狗狗口腔玩具开箱','review','好物测评,狗狗玩具,开箱','新玩具没有明显异味，材质偏软，适合短时间互动。玩完记得收起来，避免过度啃咬。','/images/demo/notes/toy-arrived.jpg',JSON_ARRAY('/images/demo/notes/toy-arrived.jpg'),72,32,0,0,23,'狗狗耐咬拉力圆环'),
('pm_seed_001','白色狗狗训练记录','dog','狗狗生活,训练,奖励','今天练习坐下和等待，口令保持短一点，动作完成马上奖励，效果比反复催促好。','/images/demo/notes/white-dog.jpg',JSON_ARRAY('/images/demo/notes/white-dog.jpg'),91,39,1,0,24,'冻干牛肝训练零食'),
('pm_seed_010','狗狗洗澡频率怎么安排','knowledge','科普知识,洗护,狗狗健康','洗澡频率要看皮肤状态、活动量和天气。洗后重点吹干毛根，耳朵和脚垫也要检查。','/images/demo/science/dog-bathing.jpg',JSON_ARRAY('/images/demo/science/dog-bathing.jpg'),105,62,1,0,25,'宠物环境除味喷雾'),
('pm_seed_002','换粮过渡第5天','knowledge','科普知识,换粮,喂养','换粮不要一步到位，旧粮比例逐步降低。便便、食欲和精神状态都稳定，再进入下一阶段。','/images/demo/science/food-transition.jpg',JSON_ARRAY('/images/demo/science/food-transition.jpg'),93,57,1,0,26,NULL),
('pm_seed_004','长毛猫掉毛季梳毛','cat','猫咪日常,掉毛,梳毛','掉毛季每天短时间梳一次，比周末集中梳更容易接受。梳完观察皮肤有没有红点。','/images/demo/science/shedding.jpg',JSON_ARRAY('/images/demo/science/shedding.jpg'),112,68,1,0,27,NULL),
('pm_seed_002','猫咪应激信号观察','knowledge','科普知识,应激,猫咪健康','突然躲藏、食欲下降、频繁舔毛都可能是应激信号。环境变化时先降低刺激，再逐步适应。','/images/demo/science/stress-response.jpg',JSON_ARRAY('/images/demo/science/stress-response.jpg'),101,59,1,0,28,NULL);

INSERT INTO `cms_note`
(`user_id`,`title`,`category`,`tags`,`content`,`cover_img`,`images`,`type`,`video_url`,`like_count`,`collect_count`,`status`,`is_deleted`,`is_recommended`,`is_sticky`,`audit_time`,`create_time`)
SELECT u.`id`,s.`title`,s.`category`,s.`tags`,s.`content`,s.`cover_img`,s.`images`,'image',NULL,
       s.`like_count`,s.`collect_count`,1,0,s.`is_recommended`,s.`is_sticky`,NOW(),
       DATE_SUB(NOW(),INTERVAL s.`created_hours_ago` HOUR)
FROM tmp_pm_seed_notes s
JOIN `sys_user` u ON u.`username`=s.`username`;

INSERT IGNORE INTO `cms_note_product_relation`(`note_id`,`product_id`)
SELECT n.`id`,p.`id`
FROM tmp_pm_seed_notes s
JOIN `sys_user` u ON u.`username`=s.`username`
JOIN `cms_note` n ON n.`user_id`=u.`id` AND n.`title`=s.`title`
JOIN `pms_product` p ON p.`name`=s.`product_name`
WHERE s.`product_name` IS NOT NULL;

-- 6.给每条新增笔记补评论、回复、点赞、收藏。
DROP TEMPORARY TABLE IF EXISTS tmp_pm_comment_templates;
CREATE TEMPORARY TABLE tmp_pm_comment_templates(
  `slot_no` INT NOT NULL,
  `username` VARCHAR(50) NOT NULL,
  `content` VARCHAR(500) NOT NULL
);

INSERT INTO tmp_pm_comment_templates VALUES
(1,'pm_seed_008','照片和文字能对上，看起来很舒服。'),
(2,'pm_seed_009','这个细节挺实用的，先收藏备用。'),
(3,'pm_seed_010','状态记录得很清楚，想看后续更新。');

INSERT INTO `cms_comment`
(`note_id`,`parent_id`,`reply_to_id`,`user_id`,`content`,`like_count`,`status`,`create_time`)
SELECT n.`id`,NULL,NULL,cu.`id`,t.`content`,MOD(n.`id`+t.`slot_no`,9),0,
       DATE_ADD(n.`create_time`,INTERVAL (t.`slot_no`*7) MINUTE)
FROM tmp_pm_seed_notes s
JOIN `sys_user` au ON au.`username`=s.`username`
JOIN `cms_note` n ON n.`user_id`=au.`id` AND n.`title`=s.`title`
JOIN tmp_pm_comment_templates t
JOIN `sys_user` cu ON cu.`username`=t.`username`;

INSERT INTO `cms_comment`
(`note_id`,`parent_id`,`reply_to_id`,`user_id`,`content`,`like_count`,`status`,`create_time`)
SELECT c.`note_id`,c.`id`,c.`id`,au.`id`,'我也会继续记录，后面有变化再补充。',MOD(c.`id`,5),0,
       DATE_ADD(c.`create_time`,INTERVAL 6 MINUTE)
FROM `cms_comment` c
JOIN `cms_note` n ON n.`id`=c.`note_id`
JOIN tmp_pm_seed_notes s ON s.`title`=n.`title`
JOIN `sys_user` au ON au.`username`=s.`username`
WHERE c.`content`='状态记录得很清楚，想看后续更新。';

INSERT IGNORE INTO `sys_interaction`(`user_id`,`target_id`,`type`,`create_time`)
SELECT u.`id`,n.`id`,1,DATE_SUB(NOW(),INTERVAL MOD(u.`id`+n.`id`,72) HOUR)
FROM `sys_user` u
JOIN `cms_note` n
JOIN tmp_pm_seed_notes s ON s.`title`=n.`title`
WHERE u.`username` LIKE 'pm_seed_%'
  AND n.`user_id` <> u.`id`
  AND MOD(u.`id`+n.`id`,3)=0;

INSERT IGNORE INTO `sys_interaction`(`user_id`,`target_id`,`type`,`create_time`)
SELECT u.`id`,n.`id`,2,DATE_SUB(NOW(),INTERVAL MOD(u.`id`+n.`id`,96) HOUR)
FROM `sys_user` u
JOIN `cms_note` n
JOIN tmp_pm_seed_notes s ON s.`title`=n.`title`
WHERE u.`username` LIKE 'pm_seed_%'
  AND n.`user_id` <> u.`id`
  AND MOD(u.`id`+n.`id`,5)=0;

INSERT IGNORE INTO `sys_interaction`(`user_id`,`target_id`,`type`,`create_time`)
SELECT u.`id`,c.`id`,3,DATE_SUB(NOW(),INTERVAL MOD(u.`id`+c.`id`,48) HOUR)
FROM `sys_user` u
JOIN `cms_comment` c
JOIN `cms_note` n ON n.`id`=c.`note_id`
JOIN tmp_pm_seed_notes s ON s.`title`=n.`title`
WHERE u.`username` LIKE 'pm_seed_%'
  AND c.`user_id` <> u.`id`
  AND MOD(u.`id`+c.`id`,4)=0;

UPDATE `cms_note` n
JOIN (
  SELECT n2.`id`,
         COALESCE(SUM(CASE WHEN i.`type`=1 THEN 1 ELSE 0 END),0) AS like_total,
         COALESCE(SUM(CASE WHEN i.`type`=2 THEN 1 ELSE 0 END),0) AS collect_total
  FROM `cms_note` n2
  JOIN tmp_pm_seed_notes s ON s.`title`=n2.`title`
  LEFT JOIN `sys_interaction` i ON i.`target_id`=n2.`id` AND i.`type` IN (1,2)
  GROUP BY n2.`id`
) x ON x.`id`=n.`id`
SET n.`like_count`=GREATEST(n.`like_count`,x.`like_total`),
    n.`collect_count`=GREATEST(n.`collect_count`,x.`collect_total`);

UPDATE `cms_comment` c
JOIN (
  SELECT c2.`id`,COUNT(i.`id`) AS like_total
  FROM `cms_comment` c2
  JOIN `cms_note` n ON n.`id`=c2.`note_id`
  JOIN tmp_pm_seed_notes s ON s.`title`=n.`title`
  LEFT JOIN `sys_interaction` i ON i.`target_id`=c2.`id` AND i.`type`=3
  GROUP BY c2.`id`
) x ON x.`id`=c.`id`
SET c.`like_count`=GREATEST(c.`like_count`,x.`like_total`);

-- 7.新增订单、订单明细、支付流水和售后。
DROP TEMPORARY TABLE IF EXISTS tmp_pm_seed_orders;
CREATE TEMPORARY TABLE tmp_pm_seed_orders(
  `order_sn` VARCHAR(64) NOT NULL,
  `username` VARCHAR(50) NOT NULL,
  `status` TINYINT NOT NULL,
  `product1` VARCHAR(200) NOT NULL,
  `qty1` INT NOT NULL,
  `product2` VARCHAR(200) DEFAULT NULL,
  `qty2` INT DEFAULT NULL,
  `created_days_ago` INT NOT NULL,
  `receiver` VARCHAR(50) NOT NULL,
  `phone` VARCHAR(20) NOT NULL,
  `address` VARCHAR(500) NOT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `review_score` TINYINT DEFAULT NULL,
  `review_content` VARCHAR(500) DEFAULT NULL
);

INSERT INTO tmp_pm_seed_orders VALUES
('PMD202606210001','pm_seed_001',0,'冻干牛肝训练零食',2,NULL,NULL,0,'李桃','13800061001','广东省深圳市南山区海德三道宠友公寓3栋1201','等下班后再付款',NULL,NULL),
('PMD202606210002','pm_seed_002',1,'长杆羽毛逗猫棒',1,'宠物环境除味喷雾',1,0,'陈满','13800061002','广东省深圳市南山区科技园南区猫窝花园2栋602','请放门卫',NULL,NULL),
('PMD202606210003','pm_seed_003',2,'狗狗耐咬拉力圆环',2,NULL,NULL,1,'周白','13800061003','广东省深圳市南山区后海大道白糖小区8栋1105','发货前请检查包装',NULL,NULL),
('PMD202606210004','pm_seed_004',3,'彩虹猫咪隧道',1,'冻干牛肝训练零食',1,1,'吴阿布','13800061004','广东省深圳市南山区粤海街道阳台里1栋1808','不要放驿站',5,'隧道展开很方便，猫咪当天就进去玩了。'),
('PMD202606210005','pm_seed_005',5,'宠物环境除味喷雾',1,NULL,NULL,2,'赵圆','13800061005','广东省深圳市南山区南山大道观察员公寓5栋905','申请退款中',NULL,NULL),
('PMD202606210006','pm_seed_006',3,'冻干牛肝训练零食',3,'宠物环境除味喷雾便携装',1,2,'钱禾','13800061006','广东省深圳市南山区蛇口湾慢生活花园6栋706',NULL,4,'零食分量合适，清洁喷雾味道也比较温和。'),
('PMD202606210007','pm_seed_007',1,'猫咪彩虹隧道加厚款',1,NULL,NULL,3,'孙青','13800061007','广东省深圳市南山区深圳湾花园鸟鸣苑9栋303','尽快发货',NULL,NULL),
('PMD202606210008','pm_seed_008',2,'狗狗春季轻薄外套',1,'宠物环境除味喷雾',1,3,'郑岩','13800061008','广东省深圳市南山区前海路爬宠公寓7栋1502',NULL,NULL,NULL),
('PMD202606210009','pm_seed_009',3,'狗狗新春保暖外套',1,'猫咪新年加绒背心',1,4,'王双','13800061009','广东省深圳市南山区高新南十道双拼家园10栋808','猫狗各一件',5,'两件衣服做工都不错，尺寸也合适。'),
('PMD202606210010','pm_seed_010',4,'长杆羽毛逗猫棒替换装',1,NULL,NULL,4,'林记','13800061010','广东省深圳市南山区南头街道笔记夹小区4栋401','用户主动取消',NULL,NULL),
('PMD202606210011','pm_seed_001',3,'狗狗耐咬训练圆环加厚款',1,'冻干牛肝训练零食',2,5,'李桃','13800061001','广东省深圳市南山区海德三道宠友公寓3栋1201',NULL,5,'圆环很耐玩，训练零食大小也刚好。'),
('PMD202606210012','pm_seed_002',2,'猫咪彩虹隧道加厚款',1,'宠物环境除味喷雾',1,5,'陈满','13800061002','广东省深圳市南山区科技园南区猫窝花园2栋602',NULL,NULL,NULL),
('PMD202606210013','pm_seed_003',1,'狗狗春季轻薄外套',1,NULL,NULL,6,'周白','13800061003','广东省深圳市南山区后海大道白糖小区8栋1105',NULL,NULL,NULL),
('PMD202606210014','pm_seed_004',3,'长杆羽毛逗猫棒',2,NULL,NULL,6,'吴阿布','13800061004','广东省深圳市南山区粤海街道阳台里1栋1808',NULL,4,'猫咪很爱扑，杆子弹性不错。'),
('PMD202606210015','pm_seed_005',0,'宠物环境除味喷雾',1,'冻干牛肝训练零食',1,7,'赵圆','13800061005','广东省深圳市南山区南山大道观察员公寓5栋905','先占库存',NULL,NULL),
('PMD202606210016','pm_seed_006',1,'彩虹猫咪隧道',1,NULL,NULL,7,'钱禾','13800061006','广东省深圳市南山区蛇口湾慢生活花园6栋706',NULL,NULL,NULL),
('PMD202606210017','pm_seed_007',2,'冻干牛肝训练零食',4,NULL,NULL,8,'孙青','13800061007','广东省深圳市南山区深圳湾花园鸟鸣苑9栋303',NULL,NULL,NULL),
('PMD202606210018','pm_seed_008',3,'宠物环境除味喷雾便携装',2,NULL,NULL,8,'郑岩','13800061008','广东省深圳市南山区前海路爬宠公寓7栋1502',NULL,4,'清洁后味道散得快，日常用够了。'),
('PMD202606210019','pm_seed_009',1,'狗狗耐咬拉力圆环',1,'长杆羽毛逗猫棒',1,9,'王双','13800061009','广东省深圳市南山区高新南十道双拼家园10栋808','一起发',NULL,NULL),
('PMD202606210020','pm_seed_010',2,'猫咪新年加绒背心',1,NULL,NULL,9,'林记','13800061010','广东省深圳市南山区南头街道笔记夹小区4栋401',NULL,NULL,NULL),
('PMD202606210021','pm_seed_001',3,'狗狗新春保暖外套',1,'狗狗耐咬拉力圆环',1,10,'李桃','13800061001','广东省深圳市南山区海德三道宠友公寓3栋1201',NULL,5,'衣服厚度适中，圆环玩具也很结实。'),
('PMD202606210022','pm_seed_002',0,'彩虹猫咪隧道',1,NULL,NULL,10,'陈满','13800061002','广东省深圳市南山区科技园南区猫窝花园2栋602','稍后支付',NULL,NULL),
('PMD202606210023','pm_seed_003',1,'宠物环境除味喷雾',2,NULL,NULL,11,'周白','13800061003','广东省深圳市南山区后海大道白糖小区8栋1105',NULL,NULL,NULL),
('PMD202606210024','pm_seed_004',2,'长杆羽毛逗猫棒替换装',1,'冻干牛肝训练零食',1,11,'吴阿布','13800061004','广东省深圳市南山区粤海街道阳台里1栋1808',NULL,NULL,NULL),
('PMD202606210025','pm_seed_005',3,'冻干牛肝训练零食',2,NULL,NULL,12,'赵圆','13800061005','广东省深圳市南山区南山大道观察员公寓5栋905',NULL,4,'小块训练奖励很方便，适合少量多次。'),
('PMD202606210026','pm_seed_006',5,'猫咪彩虹隧道加厚款',1,NULL,NULL,12,'钱禾','13800061006','广东省深圳市南山区蛇口湾慢生活花园6栋706','尺寸买错申请退款',NULL,NULL),
('PMD202606210027','pm_seed_007',0,'宠物环境除味喷雾便携装',1,NULL,NULL,13,'孙青','13800061007','广东省深圳市南山区深圳湾花园鸟鸣苑9栋303','待确认收货地址',NULL,NULL),
('PMD202606210028','pm_seed_008',1,'狗狗耐咬训练圆环加厚款',1,NULL,NULL,13,'郑岩','13800061008','广东省深圳市南山区前海路爬宠公寓7栋1502',NULL,NULL,NULL),
('PMD202606210029','pm_seed_009',2,'狗狗春季轻薄外套',1,'宠物环境除味喷雾',1,14,'王双','13800061009','广东省深圳市南山区高新南十道双拼家园10栋808',NULL,NULL,NULL),
('PMD202606210030','pm_seed_010',3,'长杆羽毛逗猫棒',1,'彩虹猫咪隧道',1,14,'林记','13800061010','广东省深圳市南山区南头街道笔记夹小区4栋401',NULL,5,'两样都很适合室内消耗精力。'),
('PMD202606210031','pm_seed_001',1,'冻干牛肝训练零食',1,NULL,NULL,15,'李桃','13800061001','广东省深圳市南山区海德三道宠友公寓3栋1201',NULL,NULL,NULL),
('PMD202606210032','pm_seed_002',2,'猫咪新年加绒背心',1,'长杆羽毛逗猫棒替换装',1,16,'陈满','13800061002','广东省深圳市南山区科技园南区猫窝花园2栋602',NULL,NULL,NULL),
('PMD202606210033','pm_seed_003',3,'狗狗新春保暖外套',1,NULL,NULL,17,'周白','13800061003','广东省深圳市南山区后海大道白糖小区8栋1105',NULL,4,'衣服保暖性不错，走线也整齐。'),
('PMD202606210034','pm_seed_004',1,'宠物环境除味喷雾',1,NULL,NULL,18,'吴阿布','13800061004','广东省深圳市南山区粤海街道阳台里1栋1808',NULL,NULL,NULL),
('PMD202606210035','pm_seed_005',2,'猫咪彩虹隧道加厚款',1,NULL,NULL,19,'赵圆','13800061005','广东省深圳市南山区南山大道观察员公寓5栋905',NULL,NULL,NULL),
('PMD202606210036','pm_seed_006',3,'冻干牛肝训练零食',2,'宠物环境除味喷雾',1,20,'钱禾','13800061006','广东省深圳市南山区蛇口湾慢生活花园6栋706',NULL,5,'复购款，日常训练和清洁都用得上。');

INSERT INTO `oms_order`
(`order_sn`,`user_id`,`total_amount`,`refund_amount`,`status`,`pay_type`,`pay_sn`,`trade_no`,`review_status`,`review_score`,`review_content`,`review_time`,`receiver_info`,`pay_time`,`ship_company`,`tracking_no`,`ship_time`,`receiver`,`phone`,`address`,`remark`,`user_deleted`,`admin_deleted`,`create_time`)
SELECT o.`order_sn`,u.`id`,
       ROUND(COALESCE(p1.`price`,0)*o.`qty1`+COALESCE(p2.`price`,0)*COALESCE(o.`qty2`,0),2),
       0.00,o.`status`,
       CASE WHEN o.`status`=0 THEN NULL ELSE 2 END,
       CASE WHEN o.`status`=0 THEN NULL ELSE CONCAT('PAY',o.`order_sn`) END,
       CASE WHEN o.`status`=0 THEN NULL ELSE CONCAT('MOCK',o.`order_sn`) END,
       CASE WHEN o.`status`=3 AND o.`review_score` IS NOT NULL THEN 1 ELSE 0 END,
       o.`review_score`,
       o.`review_content`,
       CASE WHEN o.`status`=3 AND o.`review_score` IS NOT NULL THEN DATE_ADD(DATE_SUB(NOW(),INTERVAL (o.`created_days_ago`*24+3) HOUR),INTERVAL 18 HOUR) ELSE NULL END,
       JSON_OBJECT('name',o.`receiver`,'phone',o.`phone`,'province','广东省','city','深圳市','region','南山区','detailAddress',SUBSTRING_INDEX(o.`address`,'南山区',-1),'address',o.`address`),
       CASE WHEN o.`status` IN (1,2,3,5) THEN DATE_ADD(DATE_SUB(NOW(),INTERVAL (o.`created_days_ago`*24+3) HOUR),INTERVAL 20 MINUTE) ELSE NULL END,
       CASE WHEN o.`status` IN (2,3) THEN '顺丰速运' ELSE NULL END,
       CASE WHEN o.`status` IN (2,3) THEN CONCAT('SF',RIGHT(o.`order_sn`,10)) ELSE NULL END,
       CASE WHEN o.`status` IN (2,3) THEN DATE_ADD(DATE_SUB(NOW(),INTERVAL (o.`created_days_ago`*24+3) HOUR),INTERVAL 8 HOUR) ELSE NULL END,
       o.`receiver`,o.`phone`,o.`address`,o.`remark`,0,0,
       DATE_SUB(NOW(),INTERVAL (o.`created_days_ago`*24+3) HOUR)
FROM tmp_pm_seed_orders o
JOIN `sys_user` u ON u.`username`=o.`username`
JOIN `pms_product` p1 ON p1.`name`=o.`product1`
LEFT JOIN `pms_product` p2 ON p2.`name`=o.`product2`;

INSERT INTO `oms_order_item`(`order_id`,`product_id`,`product_name`,`product_img`,`price`,`quantity`)
SELECT ord.`id`,p.`id`,p.`name`,p.`cover_img`,p.`price`,o.`qty1`
FROM tmp_pm_seed_orders o
JOIN `oms_order` ord ON ord.`order_sn`=o.`order_sn`
JOIN `pms_product` p ON p.`name`=o.`product1`;

INSERT INTO `oms_order_item`(`order_id`,`product_id`,`product_name`,`product_img`,`price`,`quantity`)
SELECT ord.`id`,p.`id`,p.`name`,p.`cover_img`,p.`price`,o.`qty2`
FROM tmp_pm_seed_orders o
JOIN `oms_order` ord ON ord.`order_sn`=o.`order_sn`
JOIN `pms_product` p ON p.`name`=o.`product2`
WHERE o.`product2` IS NOT NULL AND o.`qty2` IS NOT NULL;

INSERT INTO `oms_pay_log`
(`pay_sn`,`order_id`,`order_sn`,`user_id`,`pay_type`,`pay_mode`,`pay_amount`,`pay_status`,`trade_no`,`qr_code_url`,`pay_page_url`,`expire_time`,`pay_time`,`callback_time`,`callback_content`,`error_msg`,`create_time`,`update_time`)
SELECT CONCAT('PAY',ord.`order_sn`),ord.`id`,ord.`order_sn`,ord.`user_id`,2,1,ord.`total_amount`,
       CASE WHEN ord.`status`=4 THEN 3 ELSE 1 END,
       CONCAT('MOCK',ord.`order_sn`),
       CONCAT('mock://pay/',ord.`order_sn`),
       NULL,
       DATE_ADD(ord.`create_time`,INTERVAL 30 MINUTE),
       ord.`pay_time`,
       CASE WHEN ord.`pay_time` IS NULL THEN NULL ELSE DATE_ADD(ord.`pay_time`,INTERVAL 1 MINUTE) END,
       CASE WHEN ord.`pay_time` IS NULL THEN NULL ELSE '{"mock":"success"}' END,
       CASE WHEN ord.`status`=4 THEN '订单已关闭' ELSE NULL END,
       ord.`create_time`,
       ord.`pay_time`
FROM `oms_order` ord
WHERE ord.`order_sn` LIKE 'PMD20260621%' AND ord.`status`<>0;

INSERT INTO `oms_after_sale`
(`order_id`,`order_item_id`,`user_id`,`type`,`reason`,`description`,`evidence_images`,`status`,`user_deleted`,`admin_deleted`,`handle_remark`,`handle_time`,`create_time`)
SELECT ord.`id`,MIN(oi.`id`),ord.`user_id`,0,
       CASE WHEN ord.`order_sn`='PMD202606210005' THEN '拍错商品' ELSE '尺寸不合适' END,
       CASE WHEN ord.`order_sn`='PMD202606210005' THEN '还未发货，希望取消后退款。' ELSE '收到前发现尺寸选错，申请仅退款。' END,
       NULL,
       CASE WHEN ord.`order_sn`='PMD202606210005' THEN 1 ELSE 0 END,
       0,0,
       CASE WHEN ord.`order_sn`='PMD202606210005' THEN '客服已受理，等待退款处理。' ELSE NULL END,
       CASE WHEN ord.`order_sn`='PMD202606210005' THEN DATE_SUB(NOW(),INTERVAL 1 DAY) ELSE NULL END,
       DATE_SUB(NOW(),INTERVAL 12 HOUR)
FROM `oms_order` ord
JOIN `oms_order_item` oi ON oi.`order_id`=ord.`id`
WHERE ord.`order_sn` IN ('PMD202606210005','PMD202606210026')
GROUP BY ord.`id`,ord.`user_id`,ord.`order_sn`;

INSERT INTO `oms_refund_log`
(`refund_sn`,`order_id`,`order_sn`,`pay_log_id`,`after_sale_id`,`user_id`,`pay_type`,`refund_amount`,`refund_reason`,`refund_status`,`trade_no`,`refund_time`,`error_msg`,`create_time`,`update_time`)
SELECT CONCAT('RF',ord.`order_sn`),ord.`id`,ord.`order_sn`,pl.`id`,af.`id`,ord.`user_id`,2,
       ord.`total_amount`,
       af.`reason`,
       0,
       NULL,NULL,NULL,
       af.`create_time`,
       af.`handle_time`
FROM `oms_order` ord
JOIN `oms_after_sale` af ON af.`order_id`=ord.`id`
LEFT JOIN `oms_pay_log` pl ON pl.`order_id`=ord.`id`
WHERE ord.`order_sn` IN ('PMD202606210005','PMD202606210026');

-- 8.补投诉、通知和看板PV。
INSERT INTO `cms_complaint`
(`note_id`,`target_type`,`comment_id`,`parent_id`,`user_id`,`reason`,`content`,`evidence_images`,`status`,`handle_remark`,`user_deleted`,`admin_deleted`,`feedback_status`,`feedback_content`,`feedback_time`,`create_time`,`handle_time`,`handler_id`)
SELECT n.`id`,'note',NULL,NULL,u.`id`,'内容与图片不符','这条内容看起来需要管理员复核。',NULL,0,NULL,0,0,0,NULL,NULL,DATE_SUB(NOW(),INTERVAL 6 HOUR),NULL,NULL
FROM `cms_note` n
JOIN `sys_user` u ON u.`username`='pm_seed_010'
WHERE n.`title`='陪伴型宠物日常随拍'
LIMIT 1;

INSERT INTO `cms_complaint`
(`note_id`,`target_type`,`comment_id`,`parent_id`,`user_id`,`reason`,`content`,`evidence_images`,`status`,`handle_remark`,`user_deleted`,`admin_deleted`,`feedback_status`,`feedback_content`,`feedback_time`,`create_time`,`handle_time`,`handler_id`)
SELECT n.`id`,'comment',c.`id`,NULL,u.`id`,'评论不友善','评论语气不太友好，希望平台看一下。',NULL,1,'已提醒用户注意社区表达。',0,0,1,'处理结果可以接受。',DATE_SUB(NOW(),INTERVAL 2 HOUR),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 3 HOUR),NULL
FROM `cms_comment` c
JOIN `cms_note` n ON n.`id`=c.`note_id`
JOIN `sys_user` u ON u.`username`='pm_seed_009'
WHERE n.`title`='柯基坐姿太会营业了'
LIMIT 1;

INSERT INTO `sys_notification`(`user_id`,`title`,`content`,`biz_type`,`biz_id`,`is_read`,`create_time`,`read_time`)
SELECT ord.`user_id`,'订单已发货',CONCAT('订单',ord.`order_sn`,'已发货，请留意物流。'),'order',ord.`id`,0,ord.`ship_time`,NULL
FROM `oms_order` ord
WHERE ord.`order_sn` LIKE 'PMD20260621%' AND ord.`status` IN (2,3);

INSERT INTO `sys_notification`(`user_id`,`title`,`content`,`biz_type`,`biz_id`,`is_read`,`create_time`,`read_time`)
SELECT ord.`user_id`,'订单支付成功',CONCAT('订单',ord.`order_sn`,'支付成功，商家会尽快处理。'),'order',ord.`id`,1,ord.`pay_time`,DATE_ADD(ord.`pay_time`,INTERVAL 15 MINUTE)
FROM `oms_order` ord
WHERE ord.`order_sn` LIKE 'PMD20260621%' AND ord.`status` IN (1,2,3,5);

INSERT INTO `sys_notification`(`user_id`,`title`,`content`,`biz_type`,`biz_id`,`is_read`,`create_time`,`read_time`)
SELECT af.`user_id`,'退款申请已提交',CONCAT('订单',ord.`order_sn`,'退款申请已提交，请等待处理。'),'order_refund',ord.`id`,0,af.`create_time`,NULL
FROM `oms_after_sale` af
JOIN `oms_order` ord ON ord.`id`=af.`order_id`
WHERE ord.`order_sn` LIKE 'PMD20260621%';

INSERT INTO `sys_notification`(`user_id`,`title`,`content`,`biz_type`,`biz_id`,`is_read`,`create_time`,`read_time`)
SELECT n.`user_id`,'笔记已发布',CONCAT('你的笔记《',n.`title`,'》已通过审核。'),'note',n.`id`,1,n.`audit_time`,DATE_ADD(n.`audit_time`,INTERVAL 5 MINUTE)
FROM `cms_note` n
JOIN tmp_pm_seed_notes s ON s.`title`=n.`title`;

UPDATE `pms_product` p
SET `related_note_count`=(
  SELECT COUNT(*) FROM `cms_note_product_relation` r WHERE r.`product_id`=p.`id`
);

INSERT INTO `sys_notification`(`user_id`,`title`,`content`,`biz_type`,`biz_id`,`is_read`,`create_time`,`read_time`)
SELECT u.`id`,'欢迎加入PetMeet','这里可以记录宠物日常、收藏养宠经验，也可以查看订单进度。','system',NULL,0,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL
FROM `sys_user` u
WHERE u.`username` LIKE 'pm_seed_%';

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

-- 执行后建议清理Redis缓存：
-- redis-cli INCR cache:note:list:ver
-- redis-cli --scan --pattern 'cache:note:list:*' | xargs -r redis-cli DEL
-- redis-cli --scan --pattern 'note:like:count:*' | xargs -r redis-cli DEL
-- redis-cli --scan --pattern 'note:like:set:*' | xargs -r redis-cli DEL
