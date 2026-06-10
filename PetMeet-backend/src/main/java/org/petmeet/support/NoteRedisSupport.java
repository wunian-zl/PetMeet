package org.petmeet.support;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.dto.NoteListPageCacheDTO;
import org.petmeet.vo.NoteListVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteRedisSupport {

    private final StringRedisTemplate redisTemplate;

    public static final String NOTE_LIST_CACHE_VERSION_KEY = "cache:note:list:ver";
    public static final String NOTE_LIST_CACHE_PREFIX = "cache:note:list:";
    public static final Duration NOTE_LIST_CACHE_TTL = Duration.ofSeconds(60);

    public static final String NOTE_LIKE_DIRTY_SET_KEY = "note:like:dirty";

    public String buildNoteListCacheKey(Integer pageNum, Integer pageSize, String keyword, Long productId, String category, String tag) {
        long ver = getNoteListCacheVersion();
        String payload = String.join("|",
                "v=" + ver,
                "p=" + (pageNum == null ? 1 : pageNum),
                "s=" + (pageSize == null ? 10 : pageSize),
                "kw=" + normalize(keyword),
                "pid=" + (productId == null ? "" : productId.toString()),
                "cat=" + normalize(category),
                "tag=" + normalize(tag));
        String md5 = DigestUtils.md5DigestAsHex(payload.getBytes(StandardCharsets.UTF_8));
        return NOTE_LIST_CACHE_PREFIX + md5;
    }

    public NoteListPageCacheDTO getNoteListPageCache(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            return JSON.parseObject(json, NoteListPageCacheDTO.class);
        } catch (Exception e) {
            log.debug("read note list cache failed: {}", key, e);
            return null;
        }
    }

    public void setNoteListPageCache(String key, NoteListPageCacheDTO dto) {
        try {
            redisTemplate.opsForValue().set(key, JSON.toJSONString(dto), NOTE_LIST_CACHE_TTL);
        } catch (Exception e) {
            log.debug("write note list cache failed: {}", key, e);
        }
    }

    public long getNoteListCacheVersion() {
        try {
            String v = redisTemplate.opsForValue().get(NOTE_LIST_CACHE_VERSION_KEY);
            if (v == null || v.isBlank()) {
                return 1L;
            }
            return Long.parseLong(v.trim());
        } catch (Exception e) {
            return 1L;
        }
    }

    public void bumpNoteListCacheVersion() {
        try {
            redisTemplate.opsForValue().increment(NOTE_LIST_CACHE_VERSION_KEY);
        } catch (Exception e) {
            log.debug("bump note list cache version failed", e);
        }
    }

    public void markLikeDirty(Long noteId) {
        if (noteId == null) {
            return;
        }
        try {
            redisTemplate.opsForSet().add(NOTE_LIKE_DIRTY_SET_KEY, noteId.toString());
        } catch (Exception e) {
            log.debug("mark like dirty failed: {}", noteId, e);
        }
    }

    public void overlayLikeCountFromRedis(List<NoteListVO> records, String likeCountKeyPrefix) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<String> keys = new ArrayList<>(records.size());
        for (NoteListVO vo : records) {
            if (vo == null || vo.getId() == null) {
                continue;
            }
            keys.add(likeCountKeyPrefix + vo.getId());
        }
        if (keys.isEmpty()) {
            return;
        }

        try {
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null || values.isEmpty()) {
                return;
            }
            int idx = 0;
            for (NoteListVO vo : records) {
                if (vo == null || vo.getId() == null) {
                    continue;
                }
                String countStr = values.get(idx++);
                if (countStr != null && !countStr.isBlank()) {
                    try {
                        vo.setLikeCount(Integer.parseInt(countStr.trim()));
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            log.debug("overlay likeCount from redis failed", e);
        }
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.isEmpty() ? "" : t;
    }
}
