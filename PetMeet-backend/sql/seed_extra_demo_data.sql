-- Extra demo data for the public deployment.
-- This script is idempotent for the titles/product names listed below.
-- Community notes intentionally use pet daily/science images only.
-- Product images are used only by product records.

SET NAMES utf8mb4;
USE `petmeet`;

DELETE FROM `cms_note`
WHERE `title` IN (
  '新猫到家第一周记录',
  '猫咪突然不爱喝水怎么办',
  '猫咪喝水观察记',
  '雨天遛狗小经验',
  '小型犬外套试穿反馈',
  '小型犬冬天出门护理',
  '仓鼠夜间活动观察',
  '猫咪隧道玩具实测',
  '猫咪钻纸箱的一天',
  '狗狗磨牙玩具怎么选',
  '狗狗磨牙期怎么陪玩',
  '猫咪耳朵清洁复盘',
  '换粮第3天记录',
  '宠物除味喷雾使用感受',
  '宠物生活区清洁记录',
  '布置一个安静的猫窝角',
  '博美散步后的护理',
  '新手养仓鼠避坑',
  '猫咪掉毛季梳毛记录',
  '狗狗训练奖励零食记录',
  '狗狗训练奖励节奏记录',
  '陪伴型宠物日常随拍'
);

DELETE FROM `pms_product`
WHERE `name` IN (
  '猫咪磨爪瓦楞板',
  '便携宠物拾便袋套装',
  '狗狗训练响片',
  '猫咪换季梳毛刷',
  '宠物外出折叠水碗'
);

INSERT INTO `cms_note`
  (`user_id`, `title`, `category`, `tags`, `content`, `cover_img`, `images`, `type`, `video_url`, `like_count`, `collect_count`, `status`, `is_deleted`, `is_recommended`, `is_sticky`, `audit_time`, `create_time`)
VALUES
  (1, '新猫到家第一周记录', 'cat', '猫咪日常,新手养猫,适应期', '新猫到家后先给它一个安静角落，不急着抱，也不要频繁打扰。观察饮水、排便和食欲，适应期会顺利很多。', '/images/demo/notes/cat-companion.jpg', JSON_ARRAY('/images/demo/notes/cat-companion.jpg'), 'image', NULL, 86, 29, 1, 0, 1, 0, NOW(), DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
  (2, '猫咪喝水观察记', 'knowledge', '科普知识,饮水,猫咪健康', '把水碗放在安静位置后，它喝水次数明显增加。猫咪饮水量变化值得长期观察，异常时要及时咨询兽医。', '/images/demo/notes/cat-companion.jpg', JSON_ARRAY('/images/demo/notes/cat-companion.jpg'), 'image', NULL, 142, 73, 1, 0, 1, 0, NOW(), DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
  (3, '雨天遛狗小经验', 'dog', '狗狗生活,雨天,出行', '雨天回家重点擦干脚垫和腹部，长毛犬要注意吹干毛根，避免潮湿引起皮肤不适。', '/images/demo/notes/white-dog.jpg', JSON_ARRAY('/images/demo/notes/white-dog.jpg'), 'image', NULL, 94, 41, 1, 0, 1, 0, NOW(), DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
  (1, '小型犬冬天出门护理', 'dog', '狗狗生活,冬季护理,小型犬', '冬天短时外出也要注意脚垫和耳朵保暖，回家后检查毛发是否潮湿。小型犬体温变化更需要留意。', '/images/demo/notes/pomeranian.jpg', JSON_ARRAY('/images/demo/notes/pomeranian.jpg'), 'image', NULL, 117, 58, 1, 0, 1, 0, NOW(), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
  (4, '仓鼠夜间活动观察', 'other', '异宠日常,仓鼠,观察记录', '夜间活动量明显更高，白天尽量少打扰。跑轮和躲避屋对它适应环境很有帮助。', '/images/demo/notes/hamster.jpg', JSON_ARRAY('/images/demo/notes/hamster.jpg'), 'image', NULL, 76, 22, 1, 0, 1, 0, NOW(), DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
  (2, '猫咪钻纸箱的一天', 'cat', '猫咪日常,陪伴,治愈', '给它留了一个纸箱角落，没想到待了一下午。猫咪喜欢有边界感的小空间，安全感会更强。', '/images/demo/notes/cat-companion.jpg', JSON_ARRAY('/images/demo/notes/cat-companion.jpg'), 'image', NULL, 168, 80, 1, 0, 1, 0, NOW(), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
  (3, '狗狗磨牙期怎么陪玩', 'knowledge', '科普知识,狗狗生活,磨牙期', '磨牙期可以多做互动消耗精力，但要观察狗狗咬合力度和情绪，避免让它养成咬手习惯。', '/images/demo/notes/white-dog.jpg', JSON_ARRAY('/images/demo/notes/white-dog.jpg'), 'image', NULL, 131, 64, 1, 0, 1, 0, NOW(), DATE_SUB(NOW(), INTERVAL 3 HOUR)),
  (4, '猫咪耳朵清洁复盘', 'knowledge', '猫咪健康,耳朵,洗护', '清洁前先观察是否有异味和异常分泌物。日常只清洁外耳廓，不要深入耳道。', '/images/demo/notes/ear-care.jpg', JSON_ARRAY('/images/demo/notes/ear-care.jpg'), 'image', NULL, 104, 52, 1, 0, 0, 0, NOW(), DATE_SUB(NOW(), INTERVAL 4 HOUR)),
  (1, '换粮第3天记录', 'cat', '换粮,猫粮,喂养记录', '按旧粮占比逐步减少的方式过渡，目前便便状态稳定。换粮期间不要同时新增太多零食。', '/images/demo/science/food-transition.jpg', JSON_ARRAY('/images/demo/science/food-transition.jpg'), 'image', NULL, 119, 60, 1, 0, 0, 0, NOW(), DATE_SUB(NOW(), INTERVAL 5 HOUR)),
  (2, '宠物生活区清洁记录', 'knowledge', '清洁,日常,健康', '猫砂盆和宠物垫周边要定期清洁，清洁后保持通风干燥。宠物生活区干净，皮肤和呼吸状态也会更稳定。', '/images/demo/science/dog-bathing.jpg', JSON_ARRAY('/images/demo/science/dog-bathing.jpg'), 'image', NULL, 91, 37, 1, 0, 0, 0, NOW(), DATE_SUB(NOW(), INTERVAL 6 HOUR)),
  (4, '布置一个安静的猫窝角', 'cat', '猫咪日常,居家,陪伴', '猫咪喜欢能观察环境又不被打扰的位置。窝旁边放抓板和小毯子，它会更愿意停留。', '/images/demo/notes/cat-companion.jpg', JSON_ARRAY('/images/demo/notes/cat-companion.jpg'), 'image', NULL, 156, 72, 1, 0, 0, 0, NOW(), DATE_SUB(NOW(), INTERVAL 7 HOUR)),
  (1, '博美散步后的护理', 'dog', '狗狗生活,博美,护理', '散步回来检查脚垫、毛发和牵引胸背，天气热时注意补水，别立刻大量进食。', '/images/demo/notes/pomeranian.jpg', JSON_ARRAY('/images/demo/notes/pomeranian.jpg'), 'image', NULL, 123, 49, 1, 0, 0, 0, NOW(), DATE_SUB(NOW(), INTERVAL 8 HOUR)),
  (3, '新手养仓鼠避坑', 'knowledge', '异宠,新手,科普知识', '笼子不要放在直晒和强噪音位置，垫料要定期更换。刚到家先减少上手频率。', '/images/demo/notes/hamster.jpg', JSON_ARRAY('/images/demo/notes/hamster.jpg'), 'image', NULL, 88, 34, 1, 0, 0, 0, NOW(), DATE_SUB(NOW(), INTERVAL 9 HOUR)),
  (4, '猫咪掉毛季梳毛记录', 'cat', '掉毛,梳毛,猫咪健康', '每天短时间梳毛比一次梳很久更容易接受。梳完给一点奖励，猫咪会更配合。', '/images/demo/science/shedding.jpg', JSON_ARRAY('/images/demo/science/shedding.jpg'), 'image', NULL, 171, 83, 1, 0, 0, 0, NOW(), DATE_SUB(NOW(), INTERVAL 10 HOUR)),
  (2, '狗狗训练奖励节奏记录', 'dog', '训练,陪伴,狗狗生活', '训练时重点是及时反馈，不是一次喂很多。完成口令后立刻奖励，狗狗更容易理解。', '/images/demo/notes/white-dog.jpg', JSON_ARRAY('/images/demo/notes/white-dog.jpg'), 'image', NULL, 137, 55, 1, 0, 0, 0, NOW(), DATE_SUB(NOW(), INTERVAL 11 HOUR)),
  (1, '陪伴型宠物日常随拍', 'other', '日常,陪伴,治愈', '不一定每天都有大事件，但这些安静陪伴的瞬间很适合记录下来。', '/images/demo/notes/cat-companion.jpg', JSON_ARRAY('/images/demo/notes/cat-companion.jpg', '/images/demo/notes/pomeranian.jpg'), 'image', NULL, 192, 96, 1, 0, 0, 0, NOW(), DATE_SUB(NOW(), INTERVAL 12 HOUR));

INSERT INTO `pms_product`
  (`category_id`, `name`, `sub_title`, `price`, `stock`, `unit`, `version`, `cover_img`, `cover_imgs`, `detail_imgs`, `description`, `status`, `is_deleted`, `create_time`, `warning_stock`, `sort_weight`, `sales`, `pet_type`, `views`, `related_note_count`)
VALUES
  (3, '猫咪磨爪瓦楞板', '加厚瓦楞纸，适合日常磨爪和休息', 26.90, 260, '个', 0, '/images/demo/products/cat-tunnel.jpg', JSON_ARRAY('/images/demo/products/cat-tunnel.jpg'), JSON_ARRAY('/images/demo/products/cat-tunnel-detail.jpg'), '适合放在猫咪常活动区域，帮助消耗精力并减少家具抓挠。', 1, 0, NOW(), 30, 68, 126, 'cat', 1560, 2),
  (2, '便携宠物拾便袋套装', '外出遛狗随身携带，抽取方便', 12.90, 500, '套', 0, '/images/demo/products/pet-deodorizer.jpg', JSON_ARRAY('/images/demo/products/pet-deodorizer.jpg'), JSON_ARRAY('/images/demo/products/pet-deodorizer-detail.jpg'), '适合日常外出和公园遛狗场景，体积小，方便挂在牵引绳上。', 1, 0, NOW(), 60, 62, 238, 'dog', 2260, 1),
  (3, '狗狗训练响片', '声音清晰，适合基础口令训练', 16.90, 320, '个', 0, '/images/demo/products/dog-ring.jpg', JSON_ARRAY('/images/demo/products/dog-ring.jpg'), JSON_ARRAY('/images/demo/products/dog-ring-detail.jpg'), '搭配零食奖励使用，适合坐下、等待、召回等基础训练。', 1, 0, NOW(), 40, 73, 184, 'dog', 1980, 2),
  (2, '猫咪换季梳毛刷', '圆润梳齿，适合日常浮毛清理', 29.90, 220, '把', 0, '/images/demo/products/cat-teaser.jpg', JSON_ARRAY('/images/demo/products/cat-teaser.jpg'), JSON_ARRAY('/images/demo/products/cat-teaser-detail.jpg'), '换季掉毛期可短时间多频次梳理，减少浮毛和毛球问题。', 1, 0, NOW(), 25, 78, 176, 'cat', 2140, 1),
  (6, '宠物外出折叠水碗', '轻量可折叠，适合短途出行补水', 18.90, 360, '个', 0, '/images/demo/products/dog-spring-coat.jpg', JSON_ARRAY('/images/demo/products/dog-spring-coat.jpg'), JSON_ARRAY('/images/demo/products/dog-spring-coat-detail.jpg'), '外出散步、短途旅行时方便携带，使用后冲洗晾干即可。', 1, 0, NOW(), 50, 66, 205, 'general', 2460, 1);

UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-01.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-01.jpg') WHERE `title` = '新猫到家第一周记录';
UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-06.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-06.jpg') WHERE `title` = '猫咪喝水观察记';
UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-02.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-02.jpg') WHERE `title` = '雨天遛狗小经验';
UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-03.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-03.jpg') WHERE `title` = '小型犬冬天出门护理';
UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-11.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-11.jpg') WHERE `title` = '仓鼠夜间活动观察';
UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-07.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-07.jpg') WHERE `title` = '猫咪钻纸箱的一天';
UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-04.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-04.jpg') WHERE `title` = '狗狗磨牙期怎么陪玩';
UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-08.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-08.jpg') WHERE `title` = '猫咪耳朵清洁复盘';
UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-09.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-09.jpg') WHERE `title` = '换粮第3天记录';
UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-15.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-15.jpg') WHERE `title` = '宠物生活区清洁记录';
UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-10.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-10.jpg') WHERE `title` = '布置一个安静的猫窝角';
UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-05.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-05.jpg') WHERE `title` = '博美散步后的护理';
UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-12.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-12.jpg') WHERE `title` = '新手养仓鼠避坑';
UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-13.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-13.jpg') WHERE `title` = '猫咪掉毛季梳毛记录';
UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-14.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-14.jpg') WHERE `title` = '狗狗训练奖励节奏记录';
UPDATE `cms_note` SET `cover_img` = '/images/demo/notes-extra/note-extra-16.jpg', `images` = JSON_ARRAY('/images/demo/notes-extra/note-extra-16.jpg') WHERE `title` = '陪伴型宠物日常随拍';
