package org.petmeet.service.impl;

import org.petmeet.common.AppException;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.petmeet.dto.CommentCreateDTO;
import org.petmeet.entity.CmsComment;
import org.petmeet.entity.CmsNote;
import org.petmeet.entity.SysInteraction;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.CmsCommentMapper;
import org.petmeet.mapper.CmsNoteMapper;
import org.petmeet.mapper.SysInteractionMapper;
import org.petmeet.service.CmsCommentService;
import org.petmeet.service.SysUserService;
import org.petmeet.vo.CommentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CmsCommentServiceImpl extends ServiceImpl<CmsCommentMapper, CmsComment> implements CmsCommentService {

    private static final int REPLY_PREVIEW_SIZE = 1;

    private final CmsNoteMapper cmsNoteMapper;
    private final SysUserService sysUserService;
    private final SysInteractionMapper interactionMapper;

    @Override
    public Page<CommentVO> pageList(Long noteId, Integer pageNum, Integer pageSize) {
        QueryWrapper<CmsComment> wrapper = new QueryWrapper<>();
        wrapper.eq("note_id", noteId)
                .isNull("parent_id")
                .apply("(status = {0} OR (status = {1} AND EXISTS (SELECT 1 FROM cms_comment r WHERE r.parent_id = cms_comment.id AND r.status = {0})))",
                        CmsComment.STATUS_NORMAL,
                        CmsComment.STATUS_DELETED)
                .orderByAsc("create_time")
                .orderByAsc("id");

        Page<CmsComment> page = new Page<>(pageNum, pageSize);
        this.page(page, wrapper);

        Page<CommentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<CmsComment> roots = page.getRecords();
        if (roots == null || roots.isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        CmsNote note = cmsNoteMapper.selectById(noteId);
        ReplyPreviewData previewData = loadReplyPreview(roots);
        voPage.setRecords(buildCommentVos(roots, note, previewData.repliesByParent, previewData.replyCounts));
        return voPage;
    }

    @Override
    public Page<CommentVO> pageReplies(Long parentId, Integer pageNum, Integer pageSize) {
        CmsComment parent = this.getById(parentId);
        if (parent == null || parent.getParentId() != null) {
            throw AppException.notFound("评论不存在");
        }

        LambdaQueryWrapper<CmsComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsComment::getParentId, parentId)
                .eq(CmsComment::getStatus, CmsComment.STATUS_NORMAL)
                .orderByAsc(CmsComment::getCreateTime)
                .orderByAsc(CmsComment::getId);

        Page<CmsComment> page = new Page<>(pageNum, pageSize);
        this.page(page, wrapper);

        Page<CommentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<CmsComment> replies = page.getRecords();
        if (replies == null || replies.isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        CmsNote note = cmsNoteMapper.selectById(parent.getNoteId());
        voPage.setRecords(buildCommentVos(replies, note, Collections.emptyMap(), Collections.emptyMap()));
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(CommentCreateDTO dto) {
        CmsNote note = cmsNoteMapper.selectById(dto.getNoteId());
        if (note == null || note.getStatus() == null || note.getStatus() != CmsNote.STATUS_PUBLISHED) {
            throw AppException.notFound("笔记不存在或未发布");
        }

        Long parentId = normalizeId(dto.getParentId());
        Long replyToId = normalizeId(dto.getReplyToId());
        if (parentId != null) {
            CmsComment parent = this.getById(parentId);
            if (parent == null || parent.getParentId() != null || !Objects.equals(parent.getNoteId(), dto.getNoteId())) {
                throw AppException.notFound("父评论不存在");
            }
            if (Integer.valueOf(CmsComment.STATUS_DELETED).equals(parent.getStatus())) {
                throw AppException.badRequest("父评论已删除");
            }

            if (replyToId == null) {
                replyToId = parentId;
            }
            CmsComment replyTo = this.getById(replyToId);
            if (replyTo == null || !Objects.equals(replyTo.getNoteId(), dto.getNoteId())) {
                throw AppException.notFound("回复目标不存在");
            }
            Long replyToRootId = replyTo.getParentId() == null ? replyTo.getId() : replyTo.getParentId();
            if (!Objects.equals(replyToRootId, parentId)) {
                throw AppException.badRequest("回复目标不属于当前评论线程");
            }
            if (Integer.valueOf(CmsComment.STATUS_DELETED).equals(replyTo.getStatus())) {
                throw AppException.badRequest("回复目标已删除");
            }
        }

        CmsComment comment = new CmsComment();
        comment.setNoteId(dto.getNoteId());
        comment.setParentId(parentId);
        comment.setReplyToId(replyToId);
        comment.setUserId(StpUtil.getLoginIdAsLong());
        comment.setContent(StrUtil.trim(dto.getContent()));
        comment.setLikeCount(0);
        comment.setStatus(CmsComment.STATUS_NORMAL);
        comment.setCreateTime(LocalDateTime.now());
        this.save(comment);
        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id) {
        CmsComment comment = this.getById(id);
        if (comment == null || Integer.valueOf(CmsComment.STATUS_DELETED).equals(comment.getStatus())) {
            throw AppException.notFound("评论不存在");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        SysUser currentUser = sysUserService.getById(userId);
        boolean isAdmin = currentUser != null && "admin".equalsIgnoreCase(currentUser.getRole());
        if (!isAdmin && !Objects.equals(userId, comment.getUserId())) {
            throw AppException.forbidden("无权删除该评论");
        }

        comment.setStatus(CmsComment.STATUS_DELETED);
        comment.setDeleteTime(LocalDateTime.now());
        this.updateById(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean toggleLike(Long id) {
        CmsComment comment = this.getById(id);
        if (comment == null || Integer.valueOf(CmsComment.STATUS_DELETED).equals(comment.getStatus())) {
            throw AppException.notFound("评论不存在");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<SysInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetId, id)
                .eq(SysInteraction::getType, SysInteraction.TYPE_LIKE_COMMENT);

        SysInteraction existing = interactionMapper.selectOne(wrapper);
        int currentCount = comment.getLikeCount() == null ? 0 : comment.getLikeCount();
        if (existing != null) {
            interactionMapper.deleteById(existing.getId());
            comment.setLikeCount(Math.max(0, currentCount - 1));
            this.updateById(comment);
            return false;
        }

        SysInteraction interaction = new SysInteraction();
        interaction.setUserId(userId);
        interaction.setTargetId(id);
        interaction.setType(SysInteraction.TYPE_LIKE_COMMENT);
        interaction.setCreateTime(LocalDateTime.now());
        interactionMapper.insert(interaction);
        comment.setLikeCount(currentCount + 1);
        this.updateById(comment);
        return true;
    }

    private ReplyPreviewData loadReplyPreview(List<CmsComment> roots) {
        Map<Long, List<CmsComment>> repliesByParent = new HashMap<>();
        Map<Long, Long> replyCounts = new HashMap<>();

        for (CmsComment root : roots) {
            Long count = this.count(new LambdaQueryWrapper<CmsComment>()
                    .eq(CmsComment::getParentId, root.getId())
                    .eq(CmsComment::getStatus, CmsComment.STATUS_NORMAL));
            replyCounts.put(root.getId(), count == null ? 0L : count);

            if (count != null && count > 0) {
                Page<CmsComment> replyPage = new Page<>(1, REPLY_PREVIEW_SIZE, false);
                LambdaQueryWrapper<CmsComment> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(CmsComment::getParentId, root.getId())
                        .eq(CmsComment::getStatus, CmsComment.STATUS_NORMAL)
                        .orderByAsc(CmsComment::getCreateTime)
                        .orderByAsc(CmsComment::getId);
                this.page(replyPage, wrapper);
                repliesByParent.put(root.getId(), replyPage.getRecords());
            }
        }

        return new ReplyPreviewData(repliesByParent, replyCounts);
    }

    private List<CommentVO> buildCommentVos(
            List<CmsComment> comments,
            CmsNote note,
            Map<Long, List<CmsComment>> repliesByParent,
            Map<Long, Long> replyCounts
    ) {
        List<CmsComment> allComments = new ArrayList<>(comments);
        repliesByParent.values().forEach(allComments::addAll);

        Map<Long, CmsComment> replyToMap = loadReplyToComments(allComments);
        Map<Long, SysUser> userMap = loadUsers(allComments, replyToMap.values());
        Set<Long> likedIds = loadLikedIds(allComments);
        Long currentUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;

        return comments.stream()
                .map(comment -> toVo(comment, note, repliesByParent, replyCounts, replyToMap, userMap, likedIds, currentUserId))
                .collect(Collectors.toList());
    }

    private CommentVO toVo(
            CmsComment comment,
            CmsNote note,
            Map<Long, List<CmsComment>> repliesByParent,
            Map<Long, Long> replyCounts,
            Map<Long, CmsComment> replyToMap,
            Map<Long, SysUser> userMap,
            Set<Long> likedIds,
            Long currentUserId
    ) {
        CommentVO vo = new CommentVO();
        BeanUtil.copyProperties(comment, vo);

        boolean deleted = Integer.valueOf(CmsComment.STATUS_DELETED).equals(comment.getStatus());
        vo.setDeleted(deleted);
        vo.setMine(currentUserId != null && Objects.equals(currentUserId, comment.getUserId()));
        vo.setAuthor(note != null && Objects.equals(note.getUserId(), comment.getUserId()));
        vo.setLiked(!deleted && likedIds.contains(comment.getId()));
        vo.setLikeCount(comment.getLikeCount() == null ? 0 : comment.getLikeCount());
        vo.setReplyCount(Math.toIntExact(replyCounts.getOrDefault(comment.getId(), 0L)));

        SysUser user = userMap.get(comment.getUserId());
        if (deleted) {
            vo.setContent(null);
            vo.setUserNickname(null);
            vo.setUserAvatar(null);
            vo.setMine(false);
        } else if (user != null) {
            vo.setUserNickname(StrUtil.blankToDefault(user.getNickname(), user.getUsername()));
            vo.setUserAvatar(user.getAvatar());
        } else {
            vo.setUserNickname("已注销用户");
            vo.setUserAvatar("");
        }

        CmsComment replyTo = comment.getReplyToId() == null ? null : replyToMap.get(comment.getReplyToId());
        if (replyTo != null && !Objects.equals(replyTo.getId(), comment.getParentId())) {
            SysUser replyToUser = userMap.get(replyTo.getUserId());
            if (replyToUser != null) {
                vo.setReplyToNickname(StrUtil.blankToDefault(replyToUser.getNickname(), replyToUser.getUsername()));
            }
            vo.setReplyToContent(Integer.valueOf(CmsComment.STATUS_DELETED).equals(replyTo.getStatus()) ? "评论已删除" : replyTo.getContent());
        }

        List<CmsComment> replies = repliesByParent.getOrDefault(comment.getId(), Collections.emptyList());
        if (!replies.isEmpty()) {
            vo.setReplies(replies.stream()
                    .map(reply -> toVo(reply, note, Collections.emptyMap(), Collections.emptyMap(), replyToMap, userMap, likedIds, currentUserId))
                    .collect(Collectors.toList()));
        } else {
            vo.setReplies(Collections.emptyList());
        }
        return vo;
    }

    private Map<Long, CmsComment> loadReplyToComments(Collection<CmsComment> comments) {
        Set<Long> replyToIds = comments.stream()
                .map(CmsComment::getReplyToId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (replyToIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return this.listByIds(replyToIds).stream()
                .collect(Collectors.toMap(CmsComment::getId, c -> c, (a, b) -> a));
    }

    private Map<Long, SysUser> loadUsers(Collection<CmsComment> comments, Collection<CmsComment> replyToComments) {
        Set<Long> userIds = new HashSet<>();
        comments.stream().map(CmsComment::getUserId).filter(Objects::nonNull).forEach(userIds::add);
        replyToComments.stream().map(CmsComment::getUserId).filter(Objects::nonNull).forEach(userIds::add);
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
    }

    private Set<Long> loadLikedIds(Collection<CmsComment> comments) {
        if (!StpUtil.isLogin()) {
            return Collections.emptySet();
        }
        Set<Long> ids = comments.stream()
                .filter(c -> !Integer.valueOf(CmsComment.STATUS_DELETED).equals(c.getStatus()))
                .map(CmsComment::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptySet();
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return interactionMapper.selectList(new LambdaQueryWrapper<SysInteraction>()
                        .select(SysInteraction::getTargetId)
                        .eq(SysInteraction::getUserId, userId)
                        .eq(SysInteraction::getType, SysInteraction.TYPE_LIKE_COMMENT)
                        .in(SysInteraction::getTargetId, ids))
                .stream()
                .map(SysInteraction::getTargetId)
                .collect(Collectors.toSet());
    }

    private Long normalizeId(Long value) {
        return value != null && value > 0 ? value : null;
    }

    private record ReplyPreviewData(Map<Long, List<CmsComment>> repliesByParent, Map<Long, Long> replyCounts) {
    }
}
