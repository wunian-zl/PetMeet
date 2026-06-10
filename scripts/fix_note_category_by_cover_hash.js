const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

const PROJECT_ROOT = path.resolve(__dirname, '..');
const SOURCE_ROOT = process.env.PETMEET_IMAGE_SOURCE;
const OUTPUT_SQL = path.join(
  PROJECT_ROOT,
  'PetMeet-backend',
  'sql',
  'migration_fix_note_category_by_cover_hash_20260208.sql'
);

const APPLY = process.argv.includes('--apply');
const MYSQL_USER = process.env.PETMEET_DB_USER || 'root';
const MYSQL_PASSWORD = process.env.PETMEET_DB_PASSWORD;
const MYSQL_DATABASE = process.env.PETMEET_DB_NAME || 'petmeet';

if (!SOURCE_ROOT) {
  throw new Error('Missing required environment variable: PETMEET_IMAGE_SOURCE');
}

if (!MYSQL_PASSWORD) {
  throw new Error('Missing required environment variable: PETMEET_DB_PASSWORD');
}

const IMAGE_EXTS = new Set(['.jpg', '.jpeg', '.png', '.webp', '.bmp', '.gif']);
const SOURCE_DIR_KEYS = {
  dog: '01_Dog_狗狗',
  cat: '02_Cat_猫咪',
  reptile: '03_Reptile_爬宠',
  bird: '04_Bird_鸟类',
  small: '05_SmallPet_小宠',
  arthropod: '06_Arthropod_节肢动物',
  other: '07_Other_其他类型宠物'
};

function walkImages(dir) {
  if (!fs.existsSync(dir)) return [];
  const files = [];
  const stack = [dir];
  while (stack.length) {
    const current = stack.pop();
    const entries = fs.readdirSync(current, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(current, entry.name);
      if (entry.isDirectory()) {
        stack.push(fullPath);
      } else {
        const ext = path.extname(entry.name).toLowerCase();
        if (IMAGE_EXTS.has(ext)) {
          files.push(fullPath);
        }
      }
    }
  }
  return files;
}

function md5File(filePath) {
  const hash = crypto.createHash('md5');
  hash.update(fs.readFileSync(filePath));
  return hash.digest('hex');
}

function containsAny(text, words) {
  if (!text) return false;
  return words.some((w) => w && text.includes(w));
}

function inferSpeciesTagFromPath(filePath) {
  const text = filePath.replace(/\//g, '\\');
  if (containsAny(text, ['仓鼠'])) return '仓鼠';
  if (containsAny(text, ['豚鼠', '荷兰猪', '天竺鼠'])) return '豚鼠';
  if (containsAny(text, ['龙猫'])) return '龙猫';
  if (containsAny(text, ['小香猪', '香猪'])) return '小香猪';
  if (containsAny(text, ['水豚'])) return '水豚';
  if (containsAny(text, ['雪貂'])) return '雪貂';
  if (containsAny(text, ['蜜袋鼯'])) return '蜜袋鼯';
  if (containsAny(text, ['六角恐龙'])) return '六角恐龙';
  if (containsAny(text, ['玄凤', '牡丹鹦鹉', '虎皮鹦鹉', '鹦鹉', '文鸟'])) return '鸟类';
  if (containsAny(text, ['守宫', '蜥蜴'])) return '蜥蜴';
  if (containsAny(text, ['玉米蛇', '猪鼻蛇', '球蟒', '蛇'])) return '爬宠';
  if (containsAny(text, ['陆龟', '乌龟', '龟'])) return '龟类';
  if (containsAny(text, ['兔子', '垂耳兔', '侏儒兔', '兔'])) return '兔子';
  if (containsAny(text, ['跳蛛', '蜘蛛', '蝎'])) return '节肢宠物';
  if (containsAny(text, ['羊驼'])) return '羊驼';
  return '小众宠物';
}

function classifySource(filePath) {
  const normalized = filePath.replace(/\//g, '\\');
  if (normalized.includes(`\\${SOURCE_DIR_KEYS.dog}\\`)) {
    return { petType: 'dog', speciesTag: '狗狗' };
  }
  if (normalized.includes(`\\${SOURCE_DIR_KEYS.cat}\\`)) {
    return { petType: 'cat', speciesTag: '猫咪' };
  }
  return { petType: 'other', speciesTag: inferSpeciesTagFromPath(normalized) };
}

function sqlEscape(value) {
  if (value == null) return 'NULL';
  return `'${String(value).replace(/\\/g, '\\\\').replace(/'/g, "''")}'`;
}

function runMysql(sql) {
  return execFileSync(
    'mysql',
    ['--default-character-set=utf8mb4', '-N', '-B', `-u${MYSQL_USER}`, `-p${MYSQL_PASSWORD}`, MYSQL_DATABASE, '-e', sql],
    { encoding: 'utf8' }
  );
}

function parseRows(output) {
  const lines = output.trim() ? output.trim().split(/\r?\n/) : [];
  return lines.map((line) => {
    const cols = line.split('\t');
    return {
      id: Number(cols[0]),
      title: cols[1] || '',
      content: cols[2] || '',
      category: (cols[3] || '').trim(),
      tags: (cols[4] || '').trim(),
      coverImg: (cols[5] || '').trim(),
      status: Number(cols[6] || 0)
    };
  });
}

function coverToLocalPath(coverImg) {
  if (!coverImg) return null;
  const raw = coverImg.trim();
  if (!raw.startsWith('/images/')) return null;
  return path.join(PROJECT_ROOT, 'uploads', raw.replace(/^\/images\//, ''));
}

function mergeTags(original, extras) {
  const set = new Set();
  String(original || '')
    .split(',')
    .map((x) => x.trim())
    .filter(Boolean)
    .forEach((x) => set.add(x));
  (extras || []).filter(Boolean).forEach((x) => set.add(x));
  return Array.from(set).slice(0, 10);
}

function rewriteForOther(title, content, speciesTag) {
  const text = `${title} ${content}`;
  const catDogHeavy = /(猫咪|猫猫|猫粮|狗狗|狗粮|猫砂盆|猫尿|狗尿|幼猫|猫犬通用|金毛|泰迪|犬)/.test(text);

  if (/^清洁省心好用｜/.test(title)) {
    return {
      title: `清洁省心好用｜${speciesTag}清洁护理体验`,
      content: `给${speciesTag}做日常清洁时，更看重温和和安全性。这次记录一下实际使用感受和注意点。`
    };
  }

  if (/^小零食很友好｜/.test(title)) {
    return {
      title: `小零食很友好｜${speciesTag}零食适口性记录`,
      content: `给${speciesTag}尝试这款小零食，接受度不错。建议少量多次，持续观察精神状态和进食反应。`
    };
  }

  if (/下单这款猫粮后/.test(title) || catDogHeavy) {
    return {
      title: `${speciesTag}日常分享｜用品与零食体验`,
      content: `这条笔记已按封面宠物类型整理为${speciesTag}相关内容，重点记录日常照护和喂食体验。`
    };
  }

  return { title, content };
}

function buildSourceHashMap() {
  const roots = Object.values(SOURCE_DIR_KEYS).map((d) => path.join(SOURCE_ROOT, d));
  const map = new Map();
  for (const dir of roots) {
    const files = walkImages(dir);
    for (const file of files) {
      const hash = md5File(file);
      if (!map.has(hash)) {
        map.set(hash, classifySource(file));
      }
    }
  }
  return map;
}

function main() {
  const sourceHashMap = buildSourceHashMap();
  const rows = parseRows(
    runMysql(
      "SELECT id,IFNULL(title,''),IFNULL(content,''),IFNULL(category,''),IFNULL(tags,''),IFNULL(cover_img,''),IFNULL(status,0) FROM cms_note"
    )
  );

  const updates = [];

  for (const row of rows) {
    const coverPath = coverToLocalPath(row.coverImg);
    if (!coverPath || !fs.existsSync(coverPath)) continue;

    const coverHash = md5File(coverPath);
    const sourceInfo = sourceHashMap.get(coverHash);
    if (!sourceInfo) continue;

    let nextCategory = row.category ? row.category.toLowerCase() : '';
    let nextTags = row.tags;
    let nextTitle = row.title;
    let nextContent = row.content;
    let changed = false;

    // 以图片来源为准修复错分
    if (sourceInfo.petType === 'other') {
      if (nextCategory !== 'other') {
        nextCategory = 'other';
        changed = true;
      }
      const rewritten = rewriteForOther(nextTitle, nextContent, sourceInfo.speciesTag);
      if (rewritten.title !== nextTitle || rewritten.content !== nextContent) {
        nextTitle = rewritten.title;
        nextContent = rewritten.content;
        changed = true;
      }
      const merged = mergeTags(nextTags, ['异宠日常', sourceInfo.speciesTag]);
      const mergedText = merged.join(',');
      if (mergedText !== (nextTags || '')) {
        nextTags = mergedText;
        changed = true;
      }
    } else if (!nextCategory) {
      // 空分类补齐
      nextCategory = sourceInfo.petType;
      const merged = mergeTags(nextTags, [sourceInfo.petType === 'cat' ? '猫咪日常' : '狗狗生活']);
      nextTags = merged.join(',');
      changed = true;
    }

    if (!changed) continue;

    updates.push({
      id: row.id,
      oldCategory: row.category || '(null)',
      newCategory: nextCategory || null,
      oldTitle: row.title,
      newTitle: nextTitle,
      oldTags: row.tags,
      newTags: nextTags,
      newContent: nextContent
    });
  }

  const sql = [];
  sql.push('SET NAMES utf8mb4;');
  sql.push('START TRANSACTION;');
  for (const item of updates) {
    sql.push(
      `UPDATE cms_note SET ` +
        `category=${sqlEscape(item.newCategory)}, ` +
        `tags=${sqlEscape(item.newTags || null)}, ` +
        `title=${sqlEscape(item.newTitle)}, ` +
        `content=${sqlEscape(item.newContent)} ` +
      `WHERE id=${item.id};`
    );
  }
  sql.push('COMMIT;');

  fs.writeFileSync(OUTPUT_SQL, sql.join('\n') + '\n', 'utf8');

  console.log(`matched_updates=${updates.length}`);
  console.log(`sql_file=${OUTPUT_SQL}`);
  updates.slice(0, 30).forEach((u) => {
    console.log(`#${u.id} ${u.oldCategory} -> ${u.newCategory} | ${u.oldTitle} -> ${u.newTitle}`);
  });

  if (APPLY && updates.length > 0) {
    execFileSync(
      'mysql',
      ['--default-character-set=utf8mb4', `-u${MYSQL_USER}`, `-p${MYSQL_PASSWORD}`, MYSQL_DATABASE, '-e', `source ${OUTPUT_SQL.replace(/\\/g, '/')}`],
      { stdio: 'inherit' }
    );
  }
}

main();
