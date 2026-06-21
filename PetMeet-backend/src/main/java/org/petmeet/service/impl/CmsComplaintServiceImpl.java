package org.petmeet.service.impl;

import org.petmeet.common.AppException;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.petmeet.dto.ComplaintDTO;
import org.petmeet.dto.ComplaintFeedbackDTO;
import org.petmeet.entity.CmsComment;
import org.petmeet.entity.CmsComplaint;
import org.petmeet.entity.CmsNote;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.CmsCommentMapper;
import org.petmeet.mapper.CmsComplaintMapper;
import org.petmeet.mapper.CmsNoteMapper;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.service.CmsComplaintService;
import org.petmeet.service.SysNotificationService;
import org.petmeet.vo.AdminComplaintVO;
import org.petmeet.vo.MyComplaintVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CmsComplaintServiceImpl extends ServiceImpl<CmsComplaintMapper, CmsComplaint> implements CmsComplaintService {

    private final CmsComplaintMapper complaintMapper;
    private final CmsNoteMapper noteMapper;
    private final CmsCommentMapper commentMapper;
    private final SysUserMapper userMapper;
    private final SysNotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitComplaint(ComplaintDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        String targetType = normalizeTargetType(dto.getTargetType());

        CmsNote note = noteMapper.selectById(dto.getNoteId());
        if (note == null) {
            throw AppException.notFound("笔记不存在");
        }

        CmsComment targetComment = null;
        if ("comment".equals(targetType)) {
            if (dto.getCommentId() == null) {
                throw AppException.badRequest("评论ID不能为空");
            }
            targetComment = commentMapper.selectById(dto.getCommentId());
            if (targetComment == null
                    || !Objects.equals(targetComment.getNoteId(), dto.getNoteId())
                    || Integer.valueOf(CmsComment.STATUS_DELETED).equals(targetComment.getStatus())) {
                throw AppException.notFound("评论不存在");
            }
            if (Objects.equals(targetComment.getUserId(), userId)) {
                throw AppException.badRequest("不能举报自己的评论");
            }
        } else if (Objects.equals(note.getUserId(), userId)) {
            throw AppException.badRequest("不能投诉自己的笔记");
        }

        CmsComplaint latest = complaintMapper.selectOne(latestComplaintWrapper(userId, dto.getNoteId(), targetType, dto.getCommentId()));
        if (latest != null) {
            if (latest.getStatus() != null && latest.getStatus() == 0) {
                throw AppException.badRequest("投诉正在处理中");
            }
            if (dto.getParentId() == null) {
                throw AppException.badRequest("该对象已被投诉");
            }
            if (!Objects.equals(dto.getParentId(), latest.getId())) {
                throw AppException.badRequest("请基于最新投诉再次提交");
            }
            Integer feedback = latest.getFeedbackStatus() == null ? 0 : latest.getFeedbackStatus();
            if (feedback == 1) {
                throw AppException.badRequest("已反馈满意的投诉不能再次提交");
            }
            if (feedback == 0) {
                latest.setFeedbackStatus(2);
                latest.setFeedbackTime(LocalDateTime.now());
                complaintMapper.updateById(latest);
            }
        } else if (dto.getParentId() != null) {
            throw AppException.badRequest("投诉链路无效");
        }

        CmsComplaint complaint = new CmsComplaint();
        complaint.setNoteId(dto.getNoteId());
        complaint.setTargetType(targetType);
        complaint.setCommentId("comment".equals(targetType) ? dto.getCommentId() : null);
        complaint.setParentId(dto.getParentId());
        complaint.setUserId(userId);
        complaint.setReason(StrUtil.trimToEmpty(dto.getReason()));
        complaint.setContent(StrUtil.trimToNull(dto.getContent()));
        complaint.setEvidenceImages(toEvidenceJson(dto.getEvidenceImages()));
        complaint.setStatus(CmsComplaint.STATUS_PENDING);
        complaint.setUserDeleted(CmsComplaint.DELETE_VISIBLE);
        complaint.setAdminDeleted(CmsComplaint.DELETE_VISIBLE);
        complaint.setFeedbackStatus(0);
        complaint.setCreateTime(LocalDateTime.now());
        complaintMapper.insert(complaint);

        String noteTitle = StrUtil.blankToDefault(note.getTitle(), "相关内容");
        String targetLabel = "comment".equals(targetType) ? "评论" : "笔记";
        notificationService.sendToUser(
                userId,
                "投诉已提交",
                "你对《" + noteTitle + "》的" + targetLabel + "投诉已提交，正在核查中。",
                "complaint",
                complaint.getId()
        );
        return complaint.getId();
    }

    @Override
    public Page<MyComplaintVO> pageMy(Integer pageNum, Integer pageSize, Integer status) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<CmsComplaint> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CmsComplaint> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsComplaint::getUserId, userId)
                .and(w -> w.isNull(CmsComplaint::getUserDeleted)
                        .or().eq(CmsComplaint::getUserDeleted, CmsComplaint.DELETE_VISIBLE));
        if (status != null) {
            wrapper.eq(CmsComplaint::getStatus, status);
        }
        wrapper.orderByDesc(CmsComplaint::getCreateTime);
        complaintMapper.selectPage(page, wrapper);

        Page<MyComplaintVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(buildMyComplaintVos(page.getRecords()));
        return voPage;
    }

    @Override
    public MyComplaintVO getMyLatestByNote(Long noteId) {
        Long userId = StpUtil.getLoginIdAsLong();
        CmsComplaint c = complaintMapper.selectOne(latestComplaintWrapper(userId, noteId, "note", null));
        List<MyComplaintVO> vos = buildMyComplaintVos(c == null ? Collections.emptyList() : List.of(c));
        return vos.isEmpty() ? null : vos.get(0);
    }

    @Override
    public MyComplaintVO getMyDetail(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        CmsComplaint c = complaintMapper.selectById(id);
        if (c == null || Integer.valueOf(CmsComplaint.DELETE_DELETED).equals(c.getUserDeleted())) {
            throw AppException.notFound("投诉不存在");
        }
        if (!Objects.equals(c.getUserId(), userId)) {
            throw AppException.forbidden("无权操作");
        }
        List<MyComplaintVO> vos = buildMyComplaintVos(List.of(c));
        return vos.isEmpty() ? null : vos.get(0);
    }

    @Override
    public void feedback(Long id, ComplaintFeedbackDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        CmsComplaint complaint = complaintMapper.selectById(id);
        if (complaint == null || Integer.valueOf(CmsComplaint.DELETE_DELETED).equals(complaint.getUserDeleted())) {
            throw AppException.notFound("投诉不存在");
        }
        if (!Objects.equals(complaint.getUserId(), userId)) {
            throw AppException.forbidden("无权操作");
        }
        if (complaint.getStatus() == null || complaint.getStatus() == 0) {
            throw AppException.badRequest("投诉正在处理中");
        }
        Integer current = complaint.getFeedbackStatus() == null ? 0 : complaint.getFeedbackStatus();
        if (current != 0) {
            throw AppException.badRequest("反馈已提交");
        }

        complaint.setFeedbackStatus(dto.getFeedbackStatus());
        complaint.setFeedbackContent(StrUtil.trimToNull(dto.getContent()));
        complaint.setFeedbackTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
    }

    @Override
    public void deleteMyComplaint(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        CmsComplaint complaint = complaintMapper.selectById(id);
        if (complaint == null || Integer.valueOf(CmsComplaint.DELETE_DELETED).equals(complaint.getUserDeleted())) {
            throw AppException.notFound("投诉不存在");
        }
        if (!Objects.equals(complaint.getUserId(), userId)) {
            throw AppException.forbidden("无权操作");
        }
        if (complaint.getStatus() == null || complaint.getStatus() == 0) {
            throw AppException.badRequest("处理中投诉不能删除");
        }
        complaint.setUserDeleted(CmsComplaint.DELETE_DELETED);
        complaintMapper.updateById(complaint);
    }

    @Override
    public Page<AdminComplaintVO> pageAdmin(Integer pageNum, Integer pageSize, Integer status, String keyword) {
        Page<CmsComplaint> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CmsComplaint> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNull(CmsComplaint::getAdminDeleted)
                .or().eq(CmsComplaint::getAdminDeleted, CmsComplaint.DELETE_VISIBLE));
        if (status != null) {
            wrapper.eq(CmsComplaint::getStatus, status);
        }
        applyKeywordFilter(wrapper, keyword);
        wrapper.orderByDesc(CmsComplaint::getCreateTime);
        complaintMapper.selectPage(page, wrapper);

        Page<AdminComplaintVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(buildAdminComplaintVos(page.getRecords()));
        return voPage;
    }

    @Override
    public void updateStatus(Long id, Integer status, String remark) {
        CmsComplaint complaint = complaintMapper.selectById(id);
        if (complaint == null || Integer.valueOf(CmsComplaint.DELETE_DELETED).equals(complaint.getAdminDeleted())) {
            throw AppException.notFound("投诉不存在");
        }
        complaint.setStatus(status);
        complaint.setHandleRemark(StrUtil.trimToNull(remark));
        complaint.setHandleTime(LocalDateTime.now());
        complaint.setHandlerId(StpUtil.getLoginIdAsLong());
        complaintMapper.updateById(complaint);

        CmsNote note = noteMapper.selectById(complaint.getNoteId());
        String noteTitle = note == null ? "相关内容" : StrUtil.blankToDefault(note.getTitle(), "相关内容");
        String targetLabel = "comment".equals(normalizeTargetType(complaint.getTargetType())) ? "评论" : "笔记";
        if (status != null) {
            String result = status == CmsComplaint.STATUS_HANDLED ? "已处理"
                    : (status == CmsComplaint.STATUS_REJECTED ? "已驳回" : "已更新");
            String extra = StrUtil.isBlank(remark) ? "" : "\n处理说明：" + remark.trim();
            notificationService.sendToUser(
                    complaint.getUserId(),
                    "投诉" + result,
                    "你对《" + noteTitle + "》的" + targetLabel + "投诉" + result + "。" + extra,
                    "complaint",
                    complaint.getId()
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminSoftDelete(Long id) {
        CmsComplaint complaint = complaintMapper.selectById(id);
        if (complaint == null || Integer.valueOf(CmsComplaint.DELETE_DELETED).equals(complaint.getAdminDeleted())) {
            throw AppException.notFound("投诉不存在");
        }
        if (!canAdminSoftDelete(complaint.getStatus())) {
            throw AppException.badRequest("仅已处理或已驳回的投诉可删除");
        }
        complaintMapper.update(null, new LambdaUpdateWrapper<CmsComplaint>()
                .eq(CmsComplaint::getId, id)
                .and(w -> w.isNull(CmsComplaint::getAdminDeleted)
                        .or().eq(CmsComplaint::getAdminDeleted, CmsComplaint.DELETE_VISIBLE))
                .set(CmsComplaint::getAdminDeleted, CmsComplaint.DELETE_DELETED));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminBatchSoftDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .forEach(this::adminSoftDelete);
    }

    private LambdaQueryWrapper<CmsComplaint> latestComplaintWrapper(Long userId, Long noteId, String targetType, Long commentId) {
        LambdaQueryWrapper<CmsComplaint> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsComplaint::getNoteId, noteId)
                .eq(CmsComplaint::getUserId, userId)
                .and(w -> w.isNull(CmsComplaint::getUserDeleted)
                        .or().eq(CmsComplaint::getUserDeleted, CmsComplaint.DELETE_VISIBLE));
        if ("comment".equals(targetType)) {
            wrapper.eq(CmsComplaint::getTargetType, "comment").eq(CmsComplaint::getCommentId, commentId);
        } else {
            wrapper.and(w -> w.isNull(CmsComplaint::getTargetType).or().eq(CmsComplaint::getTargetType, "note"))
                    .isNull(CmsComplaint::getCommentId);
        }
        wrapper.orderByDesc(CmsComplaint::getCreateTime).last("limit 1");
        return wrapper;
    }

    private void applyKeywordFilter(LambdaQueryWrapper<CmsComplaint> wrapper, String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }
        String k = keyword.trim();
        List<Long> noteIds = noteMapper.selectList(new LambdaQueryWrapper<CmsNote>()
                        .select(CmsNote::getId)
                        .like(CmsNote::getTitle, k))
                .stream().map(CmsNote::getId).toList();
        List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId)
                        .and(w -> w.like(SysUser::getUsername, k).or().like(SysUser::getNickname, k)))
                .stream().map(SysUser::getId).toList();
        List<Long> commentIds = commentMapper.selectList(new LambdaQueryWrapper<CmsComment>()
                        .select(CmsComment::getId)
                        .like(CmsComment::getContent, k))
                .stream().map(CmsComment::getId).toList();

        wrapper.and(w -> {
            boolean has = false;
            if (!noteIds.isEmpty()) {
                w.in(CmsComplaint::getNoteId, noteIds);
                has = true;
            }
            if (!userIds.isEmpty()) {
                if (has) {
                    w.or();
                }
                w.in(CmsComplaint::getUserId, userIds);
                has = true;
            }
            if (!commentIds.isEmpty()) {
                if (has) {
                    w.or();
                }
                w.in(CmsComplaint::getCommentId, commentIds);
                has = true;
            }
            if (!has) {
                w.eq(CmsComplaint::getId, -1);
            }
        });
    }

    private List<MyComplaintVO> buildMyComplaintVos(List<CmsComplaint> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        DataBundle data = loadRelatedData(records);
        List<MyComplaintVO> vos = new ArrayList<>();
        for (CmsComplaint c : records) {
            MyComplaintVO vo = new MyComplaintVO();
            vo.setId(c.getId());
            vo.setNoteId(c.getNoteId());
            vo.setTargetType(normalizeTargetType(c.getTargetType()));
            vo.setCommentId(c.getCommentId());
            vo.setParentId(c.getParentId());
            vo.setReason(c.getReason());
            vo.setContent(c.getContent());
            vo.setEvidenceImages(parseEvidenceImages(c.getEvidenceImages()));
            vo.setStatus(c.getStatus());
            vo.setCreateTime(c.getCreateTime());
            vo.setHandleTime(c.getHandleTime());
            vo.setHandleRemark(c.getHandleRemark());
            vo.setFeedbackStatus(c.getFeedbackStatus());
            vo.setFeedbackContent(c.getFeedbackContent());
            vo.setFeedbackTime(c.getFeedbackTime());

            CmsNote note = data.noteMap.get(c.getNoteId());
            if (note != null) {
                vo.setNoteTitle(note.getTitle());
                vo.setNoteCover(note.getCoverImg());
            }
            CmsComment comment = data.commentMap.get(c.getCommentId());
            if (comment != null) {
                vo.setCommentContent(comment.getContent());
                SysUser author = data.userMap.get(comment.getUserId());
                if (author != null) {
                    vo.setCommentAuthorName(displayName(author));
                }
            }
            vos.add(vo);
        }
        return vos;
    }

    private List<AdminComplaintVO> buildAdminComplaintVos(List<CmsComplaint> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        DataBundle data = loadRelatedData(records);
        List<AdminComplaintVO> vos = new ArrayList<>();
        for (CmsComplaint complaint : records) {
            AdminComplaintVO vo = new AdminComplaintVO();
            vo.setId(complaint.getId());
            vo.setNoteId(complaint.getNoteId());
            vo.setTargetType(normalizeTargetType(complaint.getTargetType()));
            vo.setCommentId(complaint.getCommentId());
            vo.setParentId(complaint.getParentId());
            vo.setReason(complaint.getReason());
            vo.setContent(complaint.getContent());
            vo.setEvidenceImages(parseEvidenceImages(complaint.getEvidenceImages()));
            vo.setStatus(complaint.getStatus());
            vo.setHandleRemark(complaint.getHandleRemark());
            vo.setFeedbackStatus(complaint.getFeedbackStatus());
            vo.setFeedbackContent(complaint.getFeedbackContent());
            vo.setFeedbackTime(complaint.getFeedbackTime());
            vo.setCreateTime(complaint.getCreateTime());
            vo.setHandleTime(complaint.getHandleTime());
            vo.setHandlerId(complaint.getHandlerId());

            CmsNote note = data.noteMap.get(complaint.getNoteId());
            if (note != null) {
                vo.setNoteTitle(note.getTitle());
                vo.setNoteAuthorId(note.getUserId());
                SysUser author = data.userMap.get(note.getUserId());
                if (author != null) {
                    vo.setNoteAuthorName(displayName(author));
                }
            }

            CmsComment comment = data.commentMap.get(complaint.getCommentId());
            if (comment != null) {
                vo.setCommentContent(comment.getContent());
                vo.setCommentAuthorId(comment.getUserId());
                vo.setCommentParentId(comment.getParentId());
                vo.setCommentReplyToId(comment.getReplyToId());
                vo.setCommentCreateTime(comment.getCreateTime());
                vo.setCommentDeleted(Integer.valueOf(CmsComment.STATUS_DELETED).equals(comment.getStatus()));
                SysUser commentAuthor = data.userMap.get(comment.getUserId());
                if (commentAuthor != null) {
                    vo.setCommentAuthorName(displayName(commentAuthor));
                }
                CmsComment parentComment = comment.getParentId() == null ? null : data.commentMap.get(comment.getParentId());
                if (parentComment != null) {
                    vo.setParentCommentContent(Integer.valueOf(CmsComment.STATUS_DELETED).equals(parentComment.getStatus())
                            ? "评论已删除"
                            : parentComment.getContent());
                    SysUser parentAuthor = data.userMap.get(parentComment.getUserId());
                    if (parentAuthor != null) {
                        vo.setParentCommentAuthorName(displayName(parentAuthor));
                    }
                }
                CmsComment replyToComment = comment.getReplyToId() == null ? null : data.commentMap.get(comment.getReplyToId());
                if (replyToComment != null && !Objects.equals(replyToComment.getId(), comment.getId())) {
                    vo.setReplyToContent(Integer.valueOf(CmsComment.STATUS_DELETED).equals(replyToComment.getStatus())
                            ? "评论已删除"
                            : replyToComment.getContent());
                    SysUser replyToAuthor = data.userMap.get(replyToComment.getUserId());
                    if (replyToAuthor != null) {
                        vo.setReplyToAuthorName(displayName(replyToAuthor));
                    }
                }
            }

            SysUser reporter = data.userMap.get(complaint.getUserId());
            if (reporter != null) {
                vo.setReporterId(reporter.getId());
                vo.setReporterName(displayName(reporter));
                vo.setReporterAvatar(reporter.getAvatar());
            }
            vos.add(vo);
        }
        return vos;
    }

    private DataBundle loadRelatedData(List<CmsComplaint> records) {
        Set<Long> noteIds = records.stream().map(CmsComplaint::getNoteId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> commentIds = records.stream().map(CmsComplaint::getCommentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = records.stream().map(CmsComplaint::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());

        List<CmsNote> notes = noteIds.isEmpty() ? Collections.emptyList() : noteMapper.selectBatchIds(noteIds);
        Map<Long, CmsNote> noteMap = notes.stream().collect(Collectors.toMap(CmsNote::getId, n -> n, (a, b) -> a));
        notes.stream().map(CmsNote::getUserId).filter(Objects::nonNull).forEach(userIds::add);

        List<CmsComment> comments = commentIds.isEmpty() ? Collections.emptyList() : commentMapper.selectBatchIds(commentIds);
        Map<Long, CmsComment> commentMap = comments.stream().collect(Collectors.toMap(CmsComment::getId, c -> c, (a, b) -> a));
        Set<Long> relatedCommentIds = comments.stream()
                .flatMap(comment -> java.util.stream.Stream.of(comment.getParentId(), comment.getReplyToId()))
                .filter(Objects::nonNull)
                .filter(id -> !commentMap.containsKey(id))
                .collect(Collectors.toSet());
        if (!relatedCommentIds.isEmpty()) {
            List<CmsComment> relatedComments = commentMapper.selectBatchIds(relatedCommentIds);
            relatedComments.forEach(comment -> commentMap.putIfAbsent(comment.getId(), comment));
        }
        commentMap.values().stream().map(CmsComment::getUserId).filter(Objects::nonNull).forEach(userIds::add);

        List<SysUser> users = userIds.isEmpty() ? Collections.emptyList() : userMapper.selectBatchIds(userIds);
        Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        return new DataBundle(noteMap, commentMap, userMap);
    }

    private String normalizeTargetType(String targetType) {
        return "comment".equalsIgnoreCase(StrUtil.trimToEmpty(targetType)) ? "comment" : "note";
    }

    private String toEvidenceJson(List<String> images) {
        List<String> safeImages = sanitizeEvidenceImages(images);
        return safeImages.isEmpty() ? null : JSON.toJSONString(safeImages);
    }

    private List<String> sanitizeEvidenceImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String image : images) {
            String url = StrUtil.trimToEmpty(image);
            if (StrUtil.isBlank(url)) {
                continue;
            }
            if (!url.startsWith("/images/")) {
                throw AppException.badRequest("投诉凭证图片路径无效");
            }
            result.add(url);
        }
        if (result.size() > 3) {
            throw AppException.badRequest("投诉凭证最多上传3张");
        }
        return new ArrayList<>(result);
    }

    private List<String> parseEvidenceImages(String evidenceJson) {
        if (StrUtil.isBlank(evidenceJson)) {
            return Collections.emptyList();
        }
        try {
            List<String> list = JSON.parseArray(evidenceJson, String.class);
            return list == null ? Collections.emptyList() : list.stream()
                    .filter(StrUtil::isNotBlank)
                    .toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String displayName(SysUser user) {
        return StrUtil.blankToDefault(user.getNickname(), user.getUsername());
    }

    private boolean canAdminSoftDelete(Integer status) {
        return status != null && (status == 1 || status == 2);
    }

    private record DataBundle(
            Map<Long, CmsNote> noteMap,
            Map<Long, CmsComment> commentMap,
            Map<Long, SysUser> userMap
    ) {
    }
}
