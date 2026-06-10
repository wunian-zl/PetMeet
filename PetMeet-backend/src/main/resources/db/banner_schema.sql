-- 商城广告位表
CREATE TABLE IF NOT EXISTS cms_banner (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(100) NULL,
  position VARCHAR(50) NOT NULL,
  slot VARCHAR(20) NOT NULL,
  image_url VARCHAR(255) NOT NULL,
  keyword VARCHAR(50) NULL,
  link_url VARCHAR(255) NULL,
  link_type VARCHAR(20) DEFAULT 'url',
  sort INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  start_time DATETIME NULL,
  end_time DATETIME NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
);

-- 示例数据：商城顶部广告位
INSERT INTO cms_banner (title, position, slot, image_url, keyword, link_url, link_type, sort, status)
VALUES
  ('新手养狗指南', 'SHOP_TOP', 'card', '/petmeetImage/Main-Commodity-image/10025.jpg', '幼犬', '/', 'internal', 1, 1),
  ('猫咪绝育必读', 'SHOP_TOP', 'card', '/petmeetImage/Main-Commodity-image/10088.jpg', '绝育', '/', 'internal', 2, 1),
  ('科学换粮法', 'SHOP_TOP', 'card', '/petmeetImage/Main-Commodity-image/10069.jpg', '换粮', '/', 'internal', 3, 1);
