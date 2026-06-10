package org.petmeet.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.dto.NoteListPageCacheDTO;
import org.petmeet.dto.NotePublishDTO;
import org.petmeet.dto.NoteProductCountDTO;
import org.petmeet.entity.CmsComment;
import org.petmeet.entity.CmsNote;
import org.petmeet.entity.CmsNoteProductRelation;
import org.petmeet.entity.PmsProduct;
import org.petmeet.entity.SysInteraction;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.CmsCommentMapper;
import org.petmeet.mapper.CmsNoteMapper;
import org.petmeet.mapper.CmsNoteProductRelationMapper;
import org.petmeet.mapper.PmsProductMapper;
import org.petmeet.mapper.SysInteractionMapper;
import org.petmeet.service.CmsNoteService;
import org.petmeet.service.SysUserService;
import org.petmeet.support.NoteRedisSupport;
import org.petmeet.support.UploadPathResolver;
import org.petmeet.vo.NoteDetailVO;
import org.petmeet.vo.NoteListVO;
import org.petmeet.vo.ProductListVO;
import org.petmeet.vo.UserInfoVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CmsNoteServiceImpl extends ServiceImpl<CmsNoteMapper, CmsNote> implements CmsNoteService {

    private final CmsNoteProductRelationMapper relationMapper;
    private final CmsCommentMapper commentMapper;
    private final PmsProductMapper pmsProductMapper;
    private final SysInteractionMapper interactionMapper;
    private final SysUserService sysUserService;
    private final StringRedisTemplate redisTemplate;
    private final NoteRedisSupport noteRedisSupport;

    private static final String NOTE_LIKE_COUNT_KEY = "note:like:count:";
    private static final String NOTE_LIKE_SET_KEY = "note:like:set:";
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("cat", "dog", "other", "review", "knowledge");
    private static final List<String> CAT_KEYWORDS = Arrays.asList(
            "猫咪", "猫猫", "猫粮", "主子", "布偶", "英短", "美短", "暹罗", "橘猫", "蓝猫", "猫");
    private static final List<String> DOG_KEYWORDS = Arrays.asList(
            "狗狗", "狗粮", "幼犬", "柴犬", "金毛", "柯基", "边牧", "萨摩", "拉布拉多", "阿拉斯加", "犬", "狗");
    private static final List<String> OTHER_KEYWORDS = Arrays.asList(
            "仓鼠", "豚鼠", "荷兰猪", "天竺鼠", "龙猫", "小香猪", "香猪", "水豚", "雪貂", "蜜袋鼯", "六角恐龙",
            "玄凤", "牡丹鹦鹉", "虎皮鹦鹉", "鹦鹉", "文鸟", "守宫", "蜥蜴", "玉米蛇", "猪鼻蛇", "球蟒",
            "陆龟", "乌龟", "刺猬", "兔子", "垂耳兔", "侏儒兔", "跳蛛", "蜘蛛", "蝎");

    /**
     * 发布笔记
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(NotePublishDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 先清洗标签并推断最终分类
        List<String> cleanedTags = cleanAndDeduplicateTags(dto.getTags());
        String normalizedCategory = normalizeCategory(dto.getCategory());
        String textForClassify = buildTextForCategoryInfer(dto.getTitle(), dto.getContent(), cleanedTags);
        String inferredCategory = inferPetCategory(textForClassify);
        String finalCategory = resolveFinalCategory(normalizedCategory, inferredCategory);
        List<String> finalTags = enrichTags(cleanedTags, finalCategory, textForClassify);

        // 组装笔记主表数据
        CmsNote note = new CmsNote();
        note.setUserId(userId);
        note.setTitle(dto.getTitle());
        note.setContent(dto.getContent());
        note.setCategory(finalCategory);
        if (!finalTags.isEmpty()) {
            note.setTags(String.join(",", finalTags));
        }
        String type = StrUtil.blankToDefault(dto.getType(), "image").trim();
        if (!"image".equalsIgnoreCase(type) && !"video".equalsIgnoreCase(type)) {
            throw new RuntimeException("不支持的笔记类型: " + type);
        }
        note.setType(type.toLowerCase());

        // 图文\视频
        note.setCoverImg(dto.getCoverImg());
        if (dto.getImages() != null) {
            note.setImages(JSON.toJSONString(dto.getImages()));
        } else {
            note.setImages("[]");
        }
        note.setVideoUrl(dto.getVideoUrl());
        note.setLikeCount(0);
        note.setCollectCount(0);
        note.setStatus(CmsNote.STATUS_PENDING);
        note.setCreateTime(LocalDateTime.now());
        this.save(note);
        // 维护笔记和商品的关联关系
        if (dto.getProductIds() != null && !dto.getProductIds().isEmpty()) {
            Set<Long> productIds = new HashSet<>(dto.getProductIds());
            for (Long productId : productIds) {
                CmsNoteProductRelation rel = new CmsNoteProductRelation();
                rel.setNoteId(note.getId());
                rel.setProductId(productId);
                relationMapper.insert(rel);
            }
            LambdaUpdateWrapper<PmsProduct> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(PmsProduct::getId, productIds)
                    .setSql("related_note_count = related_note_count + 1");
            pmsProductMapper.update(null, updateWrapper);
        }
        // 这里做安全失效：数据库是准数据源，缓存尽力刷新即可。
        noteRedisSupport.bumpNoteListCacheVersion();
        return note.getId();
    }

    /**
     * 笔记详情
     */
    @Override
    public NoteDetailVO getDetail(Long noteId) {
        CmsNote note = this.getById(noteId);
        if (note == null)
            throw new RuntimeException("笔记不存在");

        // 未发布内容只允许作者自己查看
        Integer status = note.getStatus();
        if (status != null && status != CmsNote.STATUS_PUBLISHED) {
            if (CmsNote.STATUS_USER_DELETED == status || CmsNote.STATUS_ADMIN_SOFT_DELETED == status) {
                throw new RuntimeException("笔记已删除");
            }
            if (!StpUtil.isLogin()) {
                throw new RuntimeException(inaccessibleMessageByStatus(status));
            }
            Long userId = StpUtil.getLoginIdAsLong();
            if (!userId.equals(note.getUserId())) {
                throw new RuntimeException(inaccessibleMessageByStatus(status));
            }
        }

        // 把实体数据转换成详情 VO
        NoteDetailVO vo = new NoteDetailVO();
        BeanUtil.copyProperties(note, vo);
        vo.setStatus(note.getStatus());
        vo.setStatusDesc(getStatusDesc(note.getStatus()));
        vo.setCategory(note.getCategory());
        vo.setTags(parseTags(note.getTags()));

        if (StrUtil.isNotBlank(note.getImages())) {
            vo.setImages(JSON.parseArray(note.getImages(), String.class));
        } else {
            vo.setImages(Collections.emptyList());
        }

        UserInfoVO author = sysUserService.getUserInfoById(note.getUserId());
        vo.setAuthorNickname(author.getNickname());
        vo.setAuthorAvatar(author.getAvatar());

        List<PmsProduct> products = relationMapper.selectProductsByNoteId(noteId);
        vo.setProducts(products.stream().map(p -> {
            ProductListVO pv = new ProductListVO();
            BeanUtil.copyProperties(p, pv);
            return pv;
        }).collect(Collectors.toList()));

        Integer commentCount = commentMapper.selectCount(
                new LambdaQueryWrapper<CmsComment>().eq(CmsComment::getNoteId, noteId))
                .intValue();
        vo.setCommentCount(commentCount);

        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            vo.setLiked(hasLiked(userId, noteId));
            vo.setCollected(hasCollected(userId, noteId));
        } else {
            vo.setLiked(false);
            vo.setCollected(false);
        }

        String likeCountStr = redisTemplate.opsForValue().get(NOTE_LIKE_COUNT_KEY + noteId);
        if (likeCountStr != null) {
            vo.setLikeCount(Integer.parseInt(likeCountStr));
        }
        return vo;
    }

    /**
     * 笔记列表
     */
    @Override
    public Page<NoteListVO> pageList(Integer pageNum, Integer pageSize, String keyword, Long productId, String category, String tag) {
        // 先查缓存，命中就直接返回
        String cacheKey = noteRedisSupport.buildNoteListCacheKey(pageNum, pageSize, keyword, productId, category, tag);
        NoteListPageCacheDTO cached = noteRedisSupport.getNoteListPageCache(cacheKey);
        if (cached != null && cached.getRecords() != null) {
            Page<NoteListVO> voPage = new Page<>(cached.getCurrent(), cached.getSize(), cached.getTotal());
            List<NoteListVO> records = cached.getRecords();
            for (NoteListVO vo : records) {
                if (vo != null && vo.getLiked() == null) {
                    vo.setLiked(false);
                }
                normalizeCoverThumb(vo);
            }
            noteRedisSupport.overlayLikeCountFromRedis(records, NOTE_LIKE_COUNT_KEY);
            if (StpUtil.isLogin()) {
                fillLikedFlagFromDb(records, StpUtil.getLoginIdAsLong());
            }
            voPage.setRecords(records);
            return voPage;
        }

        // 缓存未命中时再走数据库查询
        LambdaQueryWrapper<CmsNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsNote::getStatus, CmsNote.STATUS_PUBLISHED);

        if (StrUtil.isNotBlank(keyword)) {
            String[] keywords = keyword.trim().split("\\s+");
            wrapper.and(w -> {
                for (String k : keywords) {
                    w.and(sub -> sub.like(CmsNote::getTitle, k).or().like(CmsNote::getContent, k));
                }
            });
        }

        if (productId != null) {
            List<Long> noteIds = relationMapper.selectNoteIdsByProductId(productId);
            if (noteIds == null || noteIds.isEmpty()) {
                return new Page<>(pageNum, pageSize, 0);
            }
            wrapper.in(CmsNote::getId, noteIds);
        }

        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(CmsNote::getCategory, category.trim());
        }
        if (StrUtil.isNotBlank(tag)) {
            String[] tags = tag.split(",");
            wrapper.and(w -> {
                for (int i = 0; i < tags.length; i++) {
                    String t = tags[i].trim();
                    if (StrUtil.isBlank(t)) {
                        continue;
                    }
                    w.like(CmsNote::getTags, t);
                    if (i < tags.length - 1) {
                        w.or();
                    }
                }
            });
        }

        // 置顶和推荐要体现在用户端列表排序里。
        wrapper.orderByDesc(CmsNote::getIsSticky)
                .orderByDesc(CmsNote::getIsRecommended)
                .orderByDesc(CmsNote::getCreateTime)
                .orderByDesc(CmsNote::getId);

        Page<CmsNote> page = new Page<>(pageNum, pageSize);
        this.page(page, wrapper);

        Page<NoteListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<NoteListVO> records = toListVOBaseBatch(page.getRecords());
        noteRedisSupport.setNoteListPageCache(cacheKey, NoteListPageCacheDTO.of(page.getCurrent(), page.getSize(), page.getTotal(), records));

        noteRedisSupport.overlayLikeCountFromRedis(records, NOTE_LIKE_COUNT_KEY);
        if (StpUtil.isLogin()) {
            fillLikedFlagFromDb(records, StpUtil.getLoginIdAsLong());
        }
        voPage.setRecords(records);
        return voPage;
    }

    /**
     * 我的笔记
     */
    @Override
    public Page<NoteListVO> pageMyNotes(Integer pageNum, Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 查询当前用户自己的笔记
        LambdaQueryWrapper<CmsNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsNote::getUserId, userId)
                .notIn(CmsNote::getStatus, CmsNote.STATUS_USER_DELETED, CmsNote.STATUS_ADMIN_SOFT_DELETED)
                .orderByDesc(CmsNote::getCreateTime);

        Page<CmsNote> page = new Page<>(pageNum, pageSize);
        this.page(page, wrapper);

        Page<NoteListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        // 转成前端列表数据
        voPage.setRecords(page.getRecords().stream().map(this::toListVO).collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 我的收藏
     */
    @Override
    public Page<NoteListVO> pageMyCollectedNotes(Integer pageNum, Integer pageSize) {
        return pageByInteractionType(pageNum, pageSize, SysInteraction.TYPE_COLLECT_NOTE);
    }

    /**
     * 我的点赞
     */
    @Override
    public Page<NoteListVO> pageMyLikedNotes(Integer pageNum, Integer pageSize) {
        return pageByInteractionType(pageNum, pageSize, SysInteraction.TYPE_LIKE_NOTE);
    }

    /**
     * 切换点赞
     */
    @Override
    public boolean toggleLike(Long noteId) {
        Long userId = StpUtil.getLoginIdAsLong();
        String setKey = NOTE_LIKE_SET_KEY + noteId;
        String countKey = NOTE_LIKE_COUNT_KEY + noteId;
        LambdaQueryWrapper<SysInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetId, noteId)
                .eq(SysInteraction::getType, SysInteraction.TYPE_LIKE_NOTE);

        boolean alreadyLiked = interactionMapper.selectCount(wrapper) > 0;
        if (alreadyLiked) {
            // 已点赞时执行取消点赞
            interactionMapper.delete(wrapper);
            updateRedisLikeState(noteId, userId, setKey, countKey, false);
            return false;
        }

        // 未点赞时新增点赞记录
        SysInteraction interaction = new SysInteraction();
        interaction.setUserId(userId);
        interaction.setTargetId(noteId);
        interaction.setType(SysInteraction.TYPE_LIKE_NOTE);
        interaction.setCreateTime(LocalDateTime.now());
        interactionMapper.insert(interaction);
        updateRedisLikeState(noteId, userId, setKey, countKey, true);
        return true;
    }

    /**
     * 切换推荐
     */
    @Override
    public boolean toggleRecommend(Long noteId) {
        CmsNote note = this.getById(noteId);
        if (note == null)
            throw new RuntimeException("笔记不存在");

        // 切换推荐状态
        boolean current = Boolean.TRUE.equals(note.getIsRecommended());
        note.setIsRecommended(!current);
        this.updateById(note);
        noteRedisSupport.bumpNoteListCacheVersion();
        return !current;
    }

    /**
     * 切换收藏
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleCollect(Long noteId) {
        Long userId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<SysInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetId, noteId)
                .eq(SysInteraction::getType, SysInteraction.TYPE_COLLECT_NOTE);
        SysInteraction existing = interactionMapper.selectOne(wrapper);

        if (existing != null) {
            // 已收藏时取消收藏
            interactionMapper.deleteById(existing.getId());
            this.lambdaUpdate().eq(CmsNote::getId, noteId).setSql("collect_count = collect_count - 1").update();
            noteRedisSupport.bumpNoteListCacheVersion();
            return false;
        } else {
            // 未收藏时新增收藏
            SysInteraction interaction = new SysInteraction();
            interaction.setUserId(userId);
            interaction.setTargetId(noteId);
            interaction.setType(SysInteraction.TYPE_COLLECT_NOTE);
            interaction.setCreateTime(LocalDateTime.now());
            interactionMapper.insert(interaction);
            this.lambdaUpdate().eq(CmsNote::getId, noteId).setSql("collect_count = collect_count + 1").update();
            noteRedisSupport.bumpNoteListCacheVersion();
            return true;
        }
    }

    /**
     * 切换上下架
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleMyShelf(Long noteId) {
        Long userId = StpUtil.getLoginIdAsLong();
        CmsNote note = this.getById(noteId);
        if (note == null) {
            throw new RuntimeException("笔记不存在");
        }
        if (!userId.equals(note.getUserId())) {
            throw new RuntimeException("只能操作自己的笔记");
        }

        Integer status = note.getStatus();
        if (status == null) {
            throw new RuntimeException("笔记状态异常");
        }
        if (CmsNote.STATUS_USER_DELETED == status || CmsNote.STATUS_ADMIN_SOFT_DELETED == status) {
            throw new RuntimeException("笔记已删除，无法操作");
        }
        if (CmsNote.STATUS_SHIELDED == status) {
            throw new RuntimeException("该笔记已被管理员下架，无法自行恢复");
        }
        if (CmsNote.STATUS_PENDING == status || CmsNote.STATUS_REJECTED == status) {
            throw new RuntimeException("当前状态不支持下架/恢复");
        }
        if (CmsNote.STATUS_PUBLISHED != status && CmsNote.STATUS_USER_OFF_SHELF != status) {
            throw new RuntimeException("当前状态不支持下架/恢复");
        }

        boolean toOffShelf = CmsNote.STATUS_PUBLISHED == status;
        note.setStatus(toOffShelf ? CmsNote.STATUS_USER_OFF_SHELF : CmsNote.STATUS_PUBLISHED);
        if (toOffShelf) {
            // 下架时同步取消置顶和推荐
            note.setIsSticky(false);
            note.setIsRecommended(false);
        }
        this.updateById(note);
        noteRedisSupport.bumpNoteListCacheVersion();
        return toOffShelf;
    }

    /**
     * 删除我的笔记
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMyNote(Long noteId) {
        Long userId = StpUtil.getLoginIdAsLong();
        CmsNote note = this.getById(noteId);
        if (note == null) {
            throw new RuntimeException("笔记不存在");
        }
        if (!userId.equals(note.getUserId())) {
            throw new RuntimeException("只能删除自己的笔记");
        }
        if (CmsNote.STATUS_ADMIN_SOFT_DELETED == note.getStatus() || CmsNote.STATUS_USER_DELETED == note.getStatus()) {
            throw new RuntimeException("笔记已删除");
        }

        // 逻辑删除笔记，并清掉运营状态
        note.setStatus(CmsNote.STATUS_USER_DELETED);
        note.setIsSticky(false);
        note.setIsRecommended(false);
        this.updateById(note);
        noteRedisSupport.bumpNoteListCacheVersion();
    }

    /**
     * 同步点赞数到数据库
     */
    @Override
    public void syncLikeCountToDb() {
        // 只同步脏 noteId，避免用 KEYS 扫描把大数据量 Redis 卡住。
        List<String> batch = new ArrayList<>(500);
        try (org.springframework.data.redis.core.Cursor<String> cursor = redisTemplate.opsForSet().scan(
                NoteRedisSupport.NOTE_LIKE_DIRTY_SET_KEY,
                org.springframework.data.redis.core.ScanOptions.scanOptions().count(500).build())) {
            while (cursor.hasNext() && batch.size() < 500) {
                batch.add(cursor.next());
            }
        } catch (Exception e) {
            log.debug("sync like count skipped due to redis error", e);
            return;
        }

        for (String noteIdStr : batch) {
            if (StrUtil.isBlank(noteIdStr)) {
                continue;
            }
            Long noteId;
            try {
                noteId = Long.parseLong(noteIdStr.trim());
            } catch (Exception ignored) {
                redisTemplate.opsForSet().remove(NoteRedisSupport.NOTE_LIKE_DIRTY_SET_KEY, noteIdStr);
                continue;
            }

            String countStr = redisTemplate.opsForValue().get(NOTE_LIKE_COUNT_KEY + noteId);
            if (StrUtil.isNotBlank(countStr)) {
                try {
                    this.lambdaUpdate()
                            .eq(CmsNote::getId, noteId)
                            .set(CmsNote::getLikeCount, Integer.parseInt(countStr.trim()))
                            .update();
                } catch (Exception e) {
                    log.debug("sync likeCount to db failed: noteId={}", noteId, e);
                    continue;
                }
            }
            redisTemplate.opsForSet().remove(NoteRedisSupport.NOTE_LIKE_DIRTY_SET_KEY, noteIdStr);
        }
    }

    private Page<NoteListVO> pageByInteractionType(Integer pageNum, Integer pageSize, Integer type) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<SysInteraction> page = new Page<>(pageNum, pageSize);
        // 先查当前用户的互动记录
        LambdaQueryWrapper<SysInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getType, type)
                .orderByDesc(SysInteraction::getCreateTime);

        interactionMapper.selectPage(page, wrapper);
        List<SysInteraction> interactions = page.getRecords();
        if (interactions == null || interactions.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        }

        List<Long> noteIds = interactions.stream()
                .map(SysInteraction::getTargetId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (noteIds.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        }

        List<CmsNote> notes = this.list(new LambdaQueryWrapper<CmsNote>()
                .in(CmsNote::getId, noteIds)
                .eq(CmsNote::getStatus, CmsNote.STATUS_PUBLISHED));
        Map<Long, CmsNote> noteMap = notes.stream()
                .collect(Collectors.toMap(CmsNote::getId, n -> n, (a, b) -> a));

        // 按原互动顺序组装笔记列表
        List<NoteListVO> records = noteIds.stream()
                .map(noteMap::get)
                .filter(Objects::nonNull)
                .map(this::toListVO)
                .collect(Collectors.toList());

        Page<NoteListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    private NoteListVO toListVO(CmsNote note) {
        NoteListVO vo = new NoteListVO();
        BeanUtil.copyProperties(note, vo);
        vo.setStatus(note.getStatus());
        vo.setStatusDesc(getStatusDesc(note.getStatus()));
        vo.setCategory(note.getCategory());
        vo.setTags(parseTags(note.getTags()));
        vo.setCoverThumb(buildThumbPath(note.getCoverImg()));
        try {
            // 查询作者信息
            UserInfoVO author = sysUserService.getUserInfoById(note.getUserId());
            vo.setAuthorNickname(author.getNickname());
            vo.setAuthorAvatar(author.getAvatar());
        } catch (Exception e) {
            vo.setAuthorNickname("已注销用户");
            vo.setAuthorAvatar(""); // 可以设置一个默认头像链接
        }

        Integer productCount = relationMapper.selectCount(
                new LambdaQueryWrapper<CmsNoteProductRelation>().eq(CmsNoteProductRelation::getNoteId, note.getId()))
                .intValue();
        vo.setProductCount(productCount);

        // 登录用户补充点赞状态
        if (StpUtil.isLogin()) {
            vo.setLiked(hasLiked(StpUtil.getLoginIdAsLong(), note.getId()));
        }
        return vo;
    }

    private String buildThumbPath(String coverImg) {
        if (StrUtil.isBlank(coverImg)) {
            return null;
        }
        // 优先返回缩略图，减少列表加载压力
        String img = coverImg.trim();
        if (img.startsWith("http://") || img.startsWith("https://")) {
            return img;
        }
        int dot = img.lastIndexOf('.');
        if (dot <= 0 || dot == img.length() - 1) {
            return img;
        }
        String prefix = img.substring(0, dot);
        String ext = img.substring(dot).toLowerCase(Locale.ROOT);
        if (".jpg".equals(ext) || ".jpeg".equals(ext)) {
            String thumbPath = prefix + "_thumb.jpg";
            if (virtualImageExists(thumbPath)) {
                return thumbPath;
            }
            return img;
        }
        return img;
    }

    private void normalizeCoverThumb(NoteListVO vo) {
        if (vo == null) {
            return;
        }
        String coverThumb = vo.getCoverThumb();
        if (StrUtil.isBlank(coverThumb)) {
            return;
        }
        if (!virtualImageExists(coverThumb)) {
            vo.setCoverThumb(vo.getCoverImg());
        }
    }

    private boolean virtualImageExists(String virtualPath) {
        if (StrUtil.isBlank(virtualPath)) {
            return false;
        }
        // 远程图片默认视为可用
        String path = virtualPath.trim().replace("\\", "/");
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return true;
        }
        if (!path.startsWith("/images/")) {
            return true;
        }

        String relative = path.substring("/images/".length());
        if (StrUtil.isBlank(relative)) {
            return false;
        }

        File uploadsRoot = resolveUploadsRootDir();
        File target = new File(uploadsRoot, relative.replace("/", File.separator));
        return target.isFile();
    }

    private File resolveUploadsRootDir() {
        return UploadPathResolver.resolveUploadsRootDir();
    }

    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case CmsNote.STATUS_PENDING -> "审核中";
            case CmsNote.STATUS_PUBLISHED -> "已发布";
            case CmsNote.STATUS_SHIELDED -> "已屏蔽";
            case CmsNote.STATUS_REJECTED -> "已拒绝";
            case CmsNote.STATUS_USER_OFF_SHELF -> "已下架";
            case CmsNote.STATUS_USER_DELETED -> "已删除";
            case CmsNote.STATUS_ADMIN_SOFT_DELETED -> "已删除";
            default -> "未知";
        };
    }

    private String inaccessibleMessageByStatus(Integer status) {
        if (status == null) {
            return "笔记暂不可见";
        }
        return switch (status) {
            case CmsNote.STATUS_PENDING -> "笔记审核中";
            case CmsNote.STATUS_SHIELDED -> "笔记已下架";
            case CmsNote.STATUS_REJECTED -> "笔记未通过审核";
            case CmsNote.STATUS_USER_OFF_SHELF -> "笔记已下架";
            case CmsNote.STATUS_USER_DELETED, CmsNote.STATUS_ADMIN_SOFT_DELETED -> "笔记已删除";
            default -> "笔记暂不可见";
        };
    }

    private List<String> parseTags(String tags) {
        if (StrUtil.isBlank(tags)) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> cleanAndDeduplicateTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String tag : tags) {
            if (StrUtil.isBlank(tag)) {
                continue;
            }
            String clean = tag.trim();
            if (clean.length() > 20) {
                clean = clean.substring(0, 20);
            }
            if (StrUtil.isNotBlank(clean)) {
                set.add(clean);
            }
            if (set.size() >= 10) {
                break;
            }
        }
        return new ArrayList<>(set);
    }

    private String normalizeCategory(String category) {
        if (StrUtil.isBlank(category)) {
            return null;
        }
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_CATEGORIES.contains(normalized) ? normalized : null;
    }

    private String buildTextForCategoryInfer(String title, String content, List<String> tags) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(title)) {
            sb.append(title.trim()).append(' ');
        }
        if (StrUtil.isNotBlank(content)) {
            sb.append(content.trim()).append(' ');
        }
        if (tags != null && !tags.isEmpty()) {
            sb.append(String.join(" ", tags));
        }
        return sb.toString();
    }

    private String inferPetCategory(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        int otherScore = scoreByKeywords(text, OTHER_KEYWORDS);
        int catScore = scoreByKeywords(text, CAT_KEYWORDS);
        int dogScore = scoreByKeywords(text, DOG_KEYWORDS);

        if (otherScore > 0 && otherScore >= catScore && otherScore >= dogScore) {
            return "other";
        }
        if (catScore > dogScore && catScore > 0) {
            return "cat";
        }
        if (dogScore > catScore && dogScore > 0) {
            return "dog";
        }
        return null;
    }

    private int scoreByKeywords(String text, List<String> keywords) {
        if (StrUtil.isBlank(text) || keywords == null || keywords.isEmpty()) {
            return 0;
        }
        int score = 0;
        for (String keyword : keywords) {
            if (StrUtil.isNotBlank(keyword) && text.contains(keyword)) {
                score++;
            }
        }
        return score;
    }

    private String resolveFinalCategory(String normalizedCategory, String inferredCategory) {
        if (StrUtil.isBlank(normalizedCategory)) {
            return inferredCategory;
        }
        if (isPetLifeCategory(normalizedCategory)
                && StrUtil.isNotBlank(inferredCategory)
                && !normalizedCategory.equals(inferredCategory)) {
            return inferredCategory;
        }
        return normalizedCategory;
    }

    private boolean isPetLifeCategory(String category) {
        return "cat".equals(category) || "dog".equals(category) || "other".equals(category);
    }

    private List<String> enrichTags(List<String> cleanedTags, String category, String textForClassify) {
        List<String> tags = new ArrayList<>(cleanedTags == null ? Collections.emptyList() : cleanedTags);

        if ("other".equals(category)) {
            addTagIfAbsent(tags, "异宠日常");
            addTagIfAbsent(tags, detectOtherSpeciesTag(textForClassify));
        } else if ("cat".equals(category) && tags.isEmpty()) {
            addTagIfAbsent(tags, "猫咪日常");
        } else if ("dog".equals(category) && tags.isEmpty()) {
            addTagIfAbsent(tags, "狗狗生活");
        }

        if (tags.size() <= 10) {
            return tags;
        }
        return new ArrayList<>(tags.subList(0, 10));
    }

    private void addTagIfAbsent(List<String> tags, String tag) {
        if (tags == null || StrUtil.isBlank(tag)) {
            return;
        }
        for (String existing : tags) {
            if (tag.equals(existing)) {
                return;
            }
        }
        tags.add(tag);
    }

    private String detectOtherSpeciesTag(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        if (containsAny(text, "仓鼠")) {
            return "仓鼠";
        }
        if (containsAny(text, "豚鼠", "荷兰猪", "天竺鼠")) {
            return "豚鼠";
        }
        if (containsAny(text, "龙猫")) {
            return "龙猫";
        }
        if (containsAny(text, "小香猪", "香猪")) {
            return "小香猪";
        }
        if (containsAny(text, "水豚")) {
            return "水豚";
        }
        if (containsAny(text, "雪貂")) {
            return "雪貂";
        }
        if (containsAny(text, "蜜袋鼯")) {
            return "蜜袋鼯";
        }
        if (containsAny(text, "六角恐龙")) {
            return "六角恐龙";
        }
        if (containsAny(text, "玄凤", "牡丹鹦鹉", "虎皮鹦鹉", "鹦鹉", "文鸟")) {
            return "鸟类";
        }
        if (containsAny(text, "守宫", "蜥蜴")) {
            return "蜥蜴";
        }
        if (containsAny(text, "玉米蛇", "猪鼻蛇", "球蟒", "蛇")) {
            return "爬宠";
        }
        if (containsAny(text, "陆龟", "乌龟")) {
            return "龟类";
        }
        if (containsAny(text, "兔子", "垂耳兔", "侏儒兔")) {
            return "兔子";
        }
        if (containsAny(text, "跳蛛", "蜘蛛", "蝎")) {
            return "节肢宠物";
        }
        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        if (StrUtil.isBlank(text) || keywords == null || keywords.length == 0) {
            return false;
        }
        for (String keyword : keywords) {
            if (StrUtil.isNotBlank(keyword) && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasLiked(Long userId, Long noteId) {
        String setKey = NOTE_LIKE_SET_KEY + noteId;
        try {
            // Redis 命中时直接判断用户是否点过赞
            Boolean hasKey = redisTemplate.hasKey(setKey);
            if (Boolean.TRUE.equals(hasKey)) {
                return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(setKey, userId.toString()));
            }
        } catch (Exception ignored) {
        }

        LambdaQueryWrapper<SysInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetId, noteId)
                .eq(SysInteraction::getType, SysInteraction.TYPE_LIKE_NOTE);
        return interactionMapper.selectCount(wrapper) > 0;
    }

    private boolean hasCollected(Long userId, Long noteId) {
        // 从数据库判断当前用户是否已收藏
        LambdaQueryWrapper<SysInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetId, noteId)
                .eq(SysInteraction::getType, SysInteraction.TYPE_COLLECT_NOTE);
        return interactionMapper.selectCount(wrapper) > 0;
    }

    private void updateRedisLikeState(Long noteId, Long userId, String setKey, String countKey, boolean liked) {
        try {
            // 同步点赞用户集合
            if (liked) {
                redisTemplate.opsForSet().add(setKey, userId.toString());
            } else {
                redisTemplate.opsForSet().remove(setKey, userId.toString());
            }

            // 重新计算点赞数并标记脏数据
            long likeCount = interactionMapper.selectCount(new LambdaQueryWrapper<SysInteraction>()
                    .eq(SysInteraction::getTargetId, noteId)
                    .eq(SysInteraction::getType, SysInteraction.TYPE_LIKE_NOTE));
            redisTemplate.opsForValue().set(countKey, Long.toString(likeCount));
            noteRedisSupport.markLikeDirty(noteId);
        } catch (Exception e) {
            log.debug("update redis like state failed: noteId={}, userId={}", noteId, userId, e);
        }
    }

    private void fillLikedFlagFromDb(List<NoteListVO> records, Long userId) {
        if (userId == null || records == null || records.isEmpty()) {
            return;
        }
        // 批量查出当前用户已点赞的笔记
        List<Long> noteIds = records.stream()
                .filter(Objects::nonNull)
                .map(NoteListVO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (noteIds.isEmpty()) {
            return;
        }

        List<SysInteraction> liked = interactionMapper.selectList(new LambdaQueryWrapper<SysInteraction>()
                .select(SysInteraction::getTargetId)
                .eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getType, SysInteraction.TYPE_LIKE_NOTE)
                .in(SysInteraction::getTargetId, noteIds));
        Set<Long> likedNoteIds = liked.stream()
                .map(SysInteraction::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // 回填每条笔记的点赞状态
        for (NoteListVO vo : records) {
            if (vo == null || vo.getId() == null) {
                continue;
            }
            vo.setLiked(likedNoteIds.contains(vo.getId()));
        }
    }

    private List<NoteListVO> toListVOBaseBatch(List<CmsNote> notes) {
        if (notes == null || notes.isEmpty()) {
            return Collections.emptyList();
        }

        // 先收集笔记ID，后面统一查商品数量
        List<Long> noteIds = notes.stream()
                .map(CmsNote::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Integer> productCountMap = new HashMap<>();
        if (!noteIds.isEmpty()) {
            try {
                List<NoteProductCountDTO> productCounts = relationMapper.selectProductCountByNoteIds(noteIds);
                if (productCounts != null) {
                    for (NoteProductCountDTO dto : productCounts) {
                        if (dto != null && dto.getNoteId() != null) {
                            productCountMap.put(dto.getNoteId(), dto.getProductCount() == null ? 0 : dto.getProductCount());
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("batch load productCount failed", e);
            }
        }

        Set<Long> userIds = notes.stream().map(CmsNote::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            try {
                // 批量查作者信息，避免循环查库
                List<SysUser> users = sysUserService.listByIds(userIds);
                if (users != null) {
                    for (SysUser u : users) {
                        if (u != null && u.getId() != null) {
                            userMap.put(u.getId(), u);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("batch load authors failed", e);
            }
        }

        List<NoteListVO> result = new ArrayList<>(notes.size());
        for (CmsNote note : notes) {
            if (note == null) {
                continue;
            }

            // 组装单条笔记列表数据
            NoteListVO vo = new NoteListVO();
            BeanUtil.copyProperties(note, vo);
            vo.setStatus(note.getStatus());
            vo.setStatusDesc(getStatusDesc(note.getStatus()));
            vo.setCategory(note.getCategory());
            vo.setTags(parseTags(note.getTags()));
            vo.setCoverThumb(buildThumbPath(note.getCoverImg()));
            vo.setLiked(false);

            SysUser author = note.getUserId() == null ? null : userMap.get(note.getUserId());
            if (author != null) {
                vo.setAuthorNickname(author.getNickname());
                vo.setAuthorAvatar(author.getAvatar());
            } else {
                vo.setAuthorNickname("已注销用户");
                vo.setAuthorAvatar("");
            }

            Integer productCount = note.getId() == null ? 0 : productCountMap.getOrDefault(note.getId(), 0);
            vo.setProductCount(productCount);
            result.add(vo);
        }
        return result;
    }
}
