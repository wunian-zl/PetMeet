const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

const PROJECT_ROOT = path.resolve(__dirname, '..');
const SOURCE_ROOT = process.env.PETMEET_IMAGE_SOURCE;
const MYSQL_USER = process.env.PETMEET_DB_USER || 'root';
const MYSQL_PASSWORD = process.env.PETMEET_DB_PASSWORD;
const MYSQL_DATABASE = process.env.PETMEET_DB_NAME || 'petmeet';

if (!SOURCE_ROOT) {
  throw new Error('Missing required environment variable: PETMEET_IMAGE_SOURCE');
}

if (!MYSQL_PASSWORD) {
  throw new Error('Missing required environment variable: PETMEET_DB_PASSWORD');
}

const now = new Date();
const yyyy = String(now.getFullYear());
const mm = String(now.getMonth() + 1).padStart(2, '0');

const uploadsRoot = path.join(PROJECT_ROOT, 'uploads');
const noteUploadDir = path.join(uploadsRoot, 'note', 'image', yyyy, mm);
const avatarUploadDir = path.join(uploadsRoot, 'user', 'avatar', yyyy, mm);

fs.mkdirSync(noteUploadDir, { recursive: true });
fs.mkdirSync(avatarUploadDir, { recursive: true });

const IMAGE_EXTS = new Set(['.jpg', '.jpeg', '.png', '.webp', '.gif', '.bmp']);

function walkImages(dir) {
  if (!fs.existsSync(dir)) return [];
  const results = [];
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
          results.push(fullPath);
        }
      }
    }
  }
  return results;
}

const categoryDirs = {
  dog: path.join(SOURCE_ROOT, '01_Dog_狗狗'),
  cat: path.join(SOURCE_ROOT, '02_Cat_猫咪'),
  reptile: path.join(SOURCE_ROOT, '03_Reptile_爬宠'),
  bird: path.join(SOURCE_ROOT, '04_Bird_鸟类'),
  small: path.join(SOURCE_ROOT, '05_SmallPet_小宠'),
  arthropod: path.join(SOURCE_ROOT, '06_Arthropod_节肢动物'),
  other: path.join(SOURCE_ROOT, '07_Other_其他类型宠物')
};

const dogImages = walkImages(categoryDirs.dog);
const catImages = walkImages(categoryDirs.cat);
const otherImages = [
  ...walkImages(categoryDirs.reptile),
  ...walkImages(categoryDirs.bird),
  ...walkImages(categoryDirs.small),
  ...walkImages(categoryDirs.arthropod),
  ...walkImages(categoryDirs.other)
];

if (dogImages.length === 0 || catImages.length === 0) {
  throw new Error('No dog/cat images found. Check petmeetImage path.');
}

const noteCopier = (() => {
  const cache = new Map();
  const urlPrefix = `/images/note/image/${yyyy}/${mm}`;
  return (src) => {
    if (cache.has(src)) return cache.get(src);
    const ext = path.extname(src).toLowerCase();
    const filename = crypto.randomUUID().replace(/-/g, '') + ext;
    const dest = path.join(noteUploadDir, filename);
    fs.copyFileSync(src, dest);
    const url = `${urlPrefix}/${filename}`;
    cache.set(src, url);
    return url;
  };
})();

const avatarCopier = (() => {
  const cache = new Map();
  const urlPrefix = `/images/user/avatar/${yyyy}/${mm}`;
  return (src) => {
    if (cache.has(src)) return cache.get(src);
    const ext = path.extname(src).toLowerCase();
    const filename = crypto.randomUUID().replace(/-/g, '') + ext;
    const dest = path.join(avatarUploadDir, filename);
    fs.copyFileSync(src, dest);
    const url = `${urlPrefix}/${filename}`;
    cache.set(src, url);
    return url;
  };
})();

function runMysql(query) {
  return execFileSync(
    'mysql',
    [`-u${MYSQL_USER}`, `-p${MYSQL_PASSWORD}`, MYSQL_DATABASE, '-e', query],
    { encoding: 'utf8' }
  );
}

function parseMysqlTable(output) {
  const lines = output.trim().split(/\r?\n/);
  if (lines.length <= 1) return [];
  const rows = [];
  for (let i = 1; i < lines.length; i += 1) {
    const line = lines[i];
    if (!line) continue;
    rows.push(line.split('\t'));
  }
  return rows;
}

const productRows = parseMysqlTable(
  runMysql('SELECT id, name, pet_type FROM pms_product WHERE status = 1;')
);

const products = productRows.map(([id, name, petType]) => ({
  id: Number(id),
  name,
  petType: (petType || 'general').toLowerCase()
}));

const dogProducts = products.filter((p) => p.petType === 'dog');
const catProducts = products.filter((p) => p.petType === 'cat');
const generalProducts = products.filter((p) => p.petType === 'general');

const passwordHash = '$2a$10$HzDl2WMQ7vBZy08OA/2/z.0WUsb3MDAhvr5.ndTKwlXNBI7WJxKoK';

const USER_COUNT = 30;
const users = Array.from({ length: USER_COUNT }).map((_, idx) => {
  const num = String(idx + 1).padStart(3, '0');
  const username = `petlover${num}`;
  const nickname = `萌宠用户${String(idx + 1).padStart(2, '0')}`;
  const avatarSource = (idx % 2 === 0 ? catImages : dogImages)[idx % (idx % 2 === 0 ? catImages.length : dogImages.length)];
  const avatar = avatarCopier(avatarSource);
  return {
    username,
    nickname,
    avatar,
    createTime: randomDateTime(90)
  };
});

const NOTE_COUNT = 80;
const notes = [];

const templates = {
  dog: {
    plain: [
      {
        title: '遛狗日常：{breed}今天超乖',
        content: '今天带{pet}去公园放风，跑得飞快，回家秒睡。毛孩子的快乐真的会感染人。'
      },
      {
        title: '狗狗洗澡小记',
        content: '给{pet}洗澡是个体力活，但看到蓬松干净的小脸就觉得值了。'
      },
      {
        title: '狗狗的表情包合集',
        content: '{pet}今天又贡献了好多可爱表情，记录一下这个高光瞬间。'
      }
    ],
    product: [
      {
        title: '换粮体验：{product}',
        content: '最近试了商城的【{product}】，颗粒适中、适口性很好，{pet}吃得很香，真心推荐。'
      },
      {
        title: '强烈推荐这款犬粮',
        content: '给{pet}换了【{product}】后精神状态很不错，便便也更稳定，继续回购。'
      }
    ]
  },
  cat: {
    plain: [
      {
        title: '{breed}的慵懒午后',
        content: '{pet}在阳台晒太阳，伸个懒腰就能把人萌化。记录一下它的日常。'
      },
      {
        title: '猫咪今日份可爱',
        content: '{pet}今天又在我的键盘上打滚，工作被它强制暂停。'
      },
      {
        title: '主子同款姿势',
        content: '学着{pet}的睡姿也太好笑了，它才是真正的生活艺术家。'
      }
    ],
    product: [
      {
        title: '猫粮测评：{product}',
        content: '用了商城的【{product}】，猫咪很爱吃，毛发也更顺滑了，真心推荐。'
      },
      {
        title: '猫咪吃得停不下来',
        content: '【{product}】适口性非常好，主子秒空碗，准备继续囤。'
      }
    ]
  },
  other: {
    plain: [
      {
        title: '{pet}的日常记录',
        content: '今天拍到了{pet}的可爱瞬间，小众宠物也有大魅力。'
      },
      {
        title: '治愈系小宠',
        content: '看着{pet}安安静静地待着，心情也变得平和了。'
      }
    ],
    product: [
      {
        title: '{pet}好物分享：{product}',
        content: '给{pet}用了【{product}】，日常照护更省心，状态也很稳定，体验不错。'
      }
    ]
  }
};

function randomItem(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function sampleUnique(arr, count) {
  const result = [];
  const used = new Set();
  const max = Math.min(count, arr.length);
  while (result.length < max) {
    const idx = Math.floor(Math.random() * arr.length);
    if (used.has(idx)) continue;
    used.add(idx);
    result.push(arr[idx]);
  }
  return result;
}

function getBreedFromPath(filePath) {
  const parts = filePath.split(path.sep);
  const folder = parts.length > 1 ? parts[parts.length - 2] : '';
  if (!folder) return '';
  const segs = folder.split('_');
  return segs[segs.length - 1] || folder;
}

function fillTemplate(str, vars) {
  return str.replace(/\{(\w+)\}/g, (_, key) => vars[key] ?? '');
}

function normalizeOtherPetTag(petLabel) {
  const text = String(petLabel || '');
  if (!text) return '小众宠物';
  if (/(玄凤|牡丹鹦鹉|虎皮鹦鹉|鹦鹉|文鸟)/.test(text)) return '鸟类';
  if (/(仓鼠)/.test(text)) return '仓鼠';
  if (/(豚鼠|荷兰猪|天竺鼠)/.test(text)) return '豚鼠';
  if (/(龙猫)/.test(text)) return '龙猫';
  if (/(小香猪|香猪)/.test(text)) return '小香猪';
  if (/(水豚)/.test(text)) return '水豚';
  if (/(雪貂)/.test(text)) return '雪貂';
  if (/(蜜袋鼯)/.test(text)) return '蜜袋鼯';
  if (/(六角恐龙)/.test(text)) return '六角恐龙';
  if (/(守宫|蜥蜴)/.test(text)) return '蜥蜴';
  if (/(玉米蛇|猪鼻蛇|球蟒|蛇)/.test(text)) return '爬宠';
  if (/(陆龟|乌龟|龟)/.test(text)) return '龟类';
  if (/(兔子|垂耳兔|侏儒兔|兔)/.test(text)) return '兔子';
  if (/(跳蛛|蜘蛛|蝎)/.test(text)) return '节肢宠物';
  return text.length <= 10 ? text : '小众宠物';
}

function buildSeedTags(petType, petLabel, hasProduct) {
  if (petType === 'dog') {
    return ['狗狗生活', hasProduct ? '好物测评' : '日常记录'];
  }
  if (petType === 'cat') {
    return ['猫咪日常', hasProduct ? '好物测评' : '日常记录'];
  }
  const speciesTag = normalizeOtherPetTag(petLabel);
  return ['异宠日常', speciesTag, hasProduct ? '好物测评' : '日常记录'];
}

function randomDateTime(daysBack) {
  const nowTime = Date.now();
  const offset = Math.floor(Math.random() * daysBack * 24 * 60 * 60 * 1000);
  const date = new Date(nowTime - offset);
  const yyyyStr = date.getFullYear();
  const mmStr = String(date.getMonth() + 1).padStart(2, '0');
  const ddStr = String(date.getDate()).padStart(2, '0');
  const hhStr = String(date.getHours()).padStart(2, '0');
  const miStr = String(date.getMinutes()).padStart(2, '0');
  const ssStr = String(date.getSeconds()).padStart(2, '0');
  return `${yyyyStr}-${mmStr}-${ddStr} ${hhStr}:${miStr}:${ssStr}`;
}

for (let i = 0; i < NOTE_COUNT; i += 1) {
  const picker = Math.random();
  const petType = picker < 0.4 ? 'dog' : picker < 0.75 ? 'cat' : 'other';
  const imagePool = petType === 'dog' ? dogImages : petType === 'cat' ? catImages : otherImages.length ? otherImages : dogImages;
  const imagesSource = sampleUnique(imagePool, 1 + Math.floor(Math.random() * 4));
  const images = imagesSource.map((src) => noteCopier(src));
  const coverImg = images[0] || '';
  const breed = getBreedFromPath(imagesSource[0]);
  const petLabel = petType === 'dog' ? '狗狗' : petType === 'cat' ? '猫咪' : breed || '小宠';
  const category = petType === 'dog' ? 'dog' : petType === 'cat' ? 'cat' : 'other';

  let product = null;
  const relationChance = petType === 'other' ? 0.3 : 0.6;
  if (Math.random() < relationChance) {
    const pool = petType === 'dog'
      ? [...dogProducts, ...generalProducts]
      : petType === 'cat'
        ? [...catProducts, ...generalProducts]
        : generalProducts;
    if (pool.length) {
      product = randomItem(pool);
    }
  }

  const tplGroup = templates[petType];
  const tpl = product ? randomItem(tplGroup.product) : randomItem(tplGroup.plain);

  const productLabel = product
    ? (petType === 'dog' ? '狗粮' : petType === 'cat' ? '猫粮' : '宠物好物')
    : '';

  const title = fillTemplate(tpl.title, {
    breed: breed || petLabel,
    pet: petLabel,
    product: productLabel
  });

  const content = fillTemplate(tpl.content, {
    breed: breed || petLabel,
    pet: petLabel,
    product: productLabel
  });
  const tags = Array.from(new Set(buildSeedTags(petType, petLabel, Boolean(product))));

  notes.push({
    user: randomItem(users),
    title,
    content,
    category,
    tags,
    coverImg,
    images,
    likeCount: Math.floor(Math.random() * 300),
    collectCount: Math.floor(Math.random() * 120),
    createTime: randomDateTime(45),
    productId: product ? product.id : null
  });
}

function sqlEscape(value) {
  if (value === null || value === undefined) return 'NULL';
  return `'${String(value).replace(/\\/g, '\\\\').replace(/'/g, "''")}'`;
}

const sql = [];
sql.push('SET NAMES utf8mb4;');
sql.push('START TRANSACTION;');

sql.push('INSERT IGNORE INTO sys_user (username, password, nickname, avatar, role, status, create_time) VALUES');
sql.push(users.map((u) => `  (${sqlEscape(u.username)}, ${sqlEscape(passwordHash)}, ${sqlEscape(u.nickname)}, ${sqlEscape(u.avatar)}, 'user', 1, ${sqlEscape(u.createTime)})`).join(',\n') + ';');

for (const note of notes) {
  const imagesJson = JSON.stringify(note.images || []);
  const tagsText = Array.isArray(note.tags) && note.tags.length > 0 ? note.tags.join(',') : null;
  sql.push(
    `INSERT INTO cms_note (user_id, title, category, tags, content, cover_img, images, like_count, collect_count, status, create_time, type, video_url) VALUES (` +
      `(SELECT id FROM sys_user WHERE username = ${sqlEscape(note.user.username)}), ` +
      `${sqlEscape(note.title)}, ${sqlEscape(note.category)}, ${sqlEscape(tagsText)}, ${sqlEscape(note.content)}, ${sqlEscape(note.coverImg)}, ${sqlEscape(imagesJson)}, ` +
      `${note.likeCount}, ${note.collectCount}, 1, ${sqlEscape(note.createTime)}, 'image', NULL` +
    `);`
  );
  if (note.productId) {
    sql.push('SET @note_id = LAST_INSERT_ID();');
    sql.push(`INSERT INTO cms_note_product_relation (note_id, product_id) VALUES (@note_id, ${note.productId});`);
  }
}

sql.push('UPDATE pms_product p SET related_note_count = (SELECT COUNT(*) FROM cms_note_product_relation r WHERE r.product_id = p.id);');
sql.push('COMMIT;');

const sqlPath = path.join(PROJECT_ROOT, 'PetMeet-backend', 'sql', 'seed_notes_users.sql');
fs.writeFileSync(sqlPath, sql.join('\n') + '\n', 'utf8');

execFileSync(
  'mysql',
  [`-u${MYSQL_USER}`, `-p${MYSQL_PASSWORD}`, MYSQL_DATABASE],
  { input: fs.readFileSync(sqlPath), encoding: 'utf8' }
);

console.log(`Seeded users: ${users.length}`);
console.log(`Seeded notes: ${notes.length}`);
console.log(`SQL saved to: ${sqlPath}`);
console.log(`Note images copied to: ${noteUploadDir}`);
console.log(`Avatar images copied to: ${avatarUploadDir}`);
