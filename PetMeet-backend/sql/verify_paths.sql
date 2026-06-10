-- 任务 4：SQL 校验结果

-- 假设 product 表有名为 'image' 或 'detail_imgs' 的字段，这里生成 3 条测试数据
-- 这些数据展示了任务 2 修改后，数据库应该存储的“虚拟相对路径”

INSERT INTO product (name, price, image, description)
VALUES ('测试商品A-自动猫砂盆', 1299.00, '/images/test-image-01.jpg', '采用全新映射路径测试');

INSERT INTO product (name, price, image, description)
VALUES ('测试商品B-冻干鸡胸肉', 39.90, '/images/test-image-02.png', '本地Uploads文件夹资源');

INSERT INTO product (name, price, image, description)
VALUES ('测试商品C-逗猫棒', 9.90, '/images/random-uuid-1234.webp', '验证虚拟路径 /images/**');

-- 说明：前端访问时，需要拼接基准地址 http://localhost:8080
-- 最终访问地址：http://localhost:8080/images/test-image-01.jpg
