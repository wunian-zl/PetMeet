SET NAMES utf8mb4;

UPDATE cms_note
SET category = 'other',
    tags = '异宠日常,清洁护理',
    title = '清洁省心好用｜异宠清洁护理体验',
    content = '这篇内容按异宠日常场景整理，重点记录清洁护理时的温和性、安全性和实际使用感受。'
WHERE status = 1
  AND title LIKE '清洁省心好用｜%';

UPDATE cms_note
SET category = 'other',
    tags = '异宠日常,零食测评',
    title = '小零食很友好｜异宠零食适口性记录',
    content = '这篇内容按异宠喂食场景整理，记录小零食的适口性表现和日常喂食观察。'
WHERE status = 1
  AND title LIKE '小零食很友好｜%';
