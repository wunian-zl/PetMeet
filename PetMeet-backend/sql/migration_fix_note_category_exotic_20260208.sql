SET NAMES utf8mb4;

-- 1) 异宠纠偏：仅修复空分类和 pet 类（cat/dog/other），避免误伤 review/knowledge
UPDATE cms_note
SET category = 'other'
WHERE (category IS NULL OR TRIM(category) = '' OR category IN ('cat', 'dog', 'other'))
  AND CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '仓鼠|豚鼠|荷兰猪|天竺鼠|龙猫|小香猪|香猪|水豚|雪貂|蜜袋鼯|六角恐龙|玄凤|牡丹鹦鹉|虎皮鹦鹉|鹦鹉|文鸟|守宫|蜥蜴|玉米蛇|猪鼻蛇|球蟒|陆龟|乌龟|刺猬|兔子|垂耳兔|侏儒兔|跳蛛|蜘蛛|蝎';

-- 2) 空分类补齐猫类
UPDATE cms_note
SET category = 'cat'
WHERE (category IS NULL OR TRIM(category) = '')
  AND CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '猫咪|猫猫|猫粮|主子|布偶|英短|美短|暹罗|橘猫|蓝猫|猫';

-- 3) 空分类补齐狗类
UPDATE cms_note
SET category = 'dog'
WHERE (category IS NULL OR TRIM(category) = '')
  AND CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '狗狗|狗粮|幼犬|柴犬|金毛|柯基|边牧|萨摩|拉布拉多|阿拉斯加|犬|狗';

-- 4) 空标签补齐默认标签
UPDATE cms_note
SET tags = '猫咪日常'
WHERE category = 'cat' AND (tags IS NULL OR TRIM(tags) = '');

UPDATE cms_note
SET tags = '狗狗生活'
WHERE category = 'dog' AND (tags IS NULL OR TRIM(tags) = '');

UPDATE cms_note
SET tags = CASE
    WHEN CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '仓鼠' THEN '异宠日常,仓鼠'
    WHEN CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '豚鼠|荷兰猪|天竺鼠' THEN '异宠日常,豚鼠'
    WHEN CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '龙猫' THEN '异宠日常,龙猫'
    WHEN CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '小香猪|香猪' THEN '异宠日常,小香猪'
    WHEN CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '水豚' THEN '异宠日常,水豚'
    WHEN CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '雪貂' THEN '异宠日常,雪貂'
    WHEN CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '蜜袋鼯' THEN '异宠日常,蜜袋鼯'
    WHEN CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '六角恐龙' THEN '异宠日常,六角恐龙'
    WHEN CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '玄凤|牡丹鹦鹉|虎皮鹦鹉|鹦鹉|文鸟' THEN '异宠日常,鸟类'
    WHEN CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '守宫|蜥蜴' THEN '异宠日常,蜥蜴'
    WHEN CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '玉米蛇|猪鼻蛇|球蟒|蛇' THEN '异宠日常,爬宠'
    WHEN CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '陆龟|乌龟' THEN '异宠日常,龟类'
    WHEN CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '兔子|垂耳兔|侏儒兔' THEN '异宠日常,兔子'
    WHEN CONCAT(IFNULL(title,''), ' ', IFNULL(content,''), ' ', IFNULL(tags,'')) REGEXP '跳蛛|蜘蛛|蝎' THEN '异宠日常,节肢宠物'
    ELSE '异宠日常'
END
WHERE category = 'other' AND (tags IS NULL OR TRIM(tags) = '');
