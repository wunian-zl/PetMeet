package org.petmeet.service.impl;

import org.petmeet.common.AppException;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.entity.CmsComment;
import org.petmeet.entity.CmsNote;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.CmsCommentMapper;
import org.petmeet.mapper.CmsNoteMapper;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.service.AdminNoteService;
import org.petmeet.service.SysNotificationService;
import org.petmeet.support.NoteRedisSupport;
import org.petmeet.vo.AdminNoteVO;
import org.petmeet.vo.AdminNoteStatsVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNoteServiceImpl implements AdminNoteService {

    private final CmsNoteMapper cmsNoteMapper;
    private final CmsCommentMapper commentMapper;
    private final SysUserMapper sysUserMapper;
    private final SysNotificationService notificationService;
    private final NoteRedisSupport noteRedisSupport;

    /**
     * 笔记列表
     */
    @Override
    public Page<AdminNoteVO> pageList(Integer pageNum, Integer pageSize, Integer status, String keyword, String category, String tag) {
        Page<CmsNote> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CmsNote> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(CmsNote::getStatus, status);
        } else {
            // “全部”视图里默认隐藏用户删除和管理端软删除的内容。
            wrapper.notIn(CmsNote::getStatus, CmsNote.STATUS_USER_DELETED, CmsNote.STATUS_ADMIN_SOFT_DELETED);
        }
        if (StrUtil.isNotBlank(keyword)) {
            String kw = keyword.trim();

            // 支持按笔记标题、笔记 ID、作者用户名或昵称搜索。
            List<Long> userIds = sysUserMapper.selectList(
                            new LambdaQueryWrapper<SysUser>()
                                    .select(SysUser::getId)
                                    .and(w -> w.like(SysUser::getUsername, kw).or().like(SysUser::getNickname, kw)))
                    .stream()
                    .map(SysUser::getId)
                    .toList();

            Long noteId = null;
            try {
                noteId = Long.parseLong(kw);
            } catch (Exception ignored) {
            }

            final Long finalNoteId = noteId;
            wrapper.and(w -> {
                w.like(CmsNote::getTitle, kw);
                if (finalNoteId != null) {
                    w.or().eq(CmsNote::getId, finalNoteId);
                }
                if (userIds != null && !userIds.isEmpty()) {
                    w.or().in(CmsNote::getUserId, userIds);
                }
            });
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
        wrapper.orderByDesc(CmsNote::getCreateTime);

        // 查询后台笔记分页
        Page<CmsNote> notePage = cmsNoteMapper.selectPage(page, wrapper);
        // 转成后台展示对象
        Page<AdminNoteVO> voPage = new Page<>(notePage.getCurrent(), notePage.getSize(), notePage.getTotal());
        voPage.setRecords(notePage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 统计数据
     */
    @Override
    public AdminNoteStatsVO getStats() {
        AdminNoteStatsVO stats = new AdminNoteStatsVO();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        // 待审核数量 (status=0)
        stats.setPendingCount(cmsNoteMapper.selectCount(
                new LambdaQueryWrapper<CmsNote>().eq(CmsNote::getStatus, CmsNote.STATUS_PENDING)).intValue());

        // 今日新增
        stats.setTodayCount(cmsNoteMapper.selectCount(
                new LambdaQueryWrapper<CmsNote>().ge(CmsNote::getCreateTime, todayStart)).intValue());

        // 今日已通过(按审核时间统计)
        stats.setTodayApprovedCount(cmsNoteMapper.selectCount(
                new LambdaQueryWrapper<CmsNote>()
                        .eq(CmsNote::getStatus, CmsNote.STATUS_PUBLISHED)
                        .ge(CmsNote::getAuditTime, todayStart)).intValue());

        // 今日已拒绝(按审核时间统计)
        stats.setTodayRejectedCount(cmsNoteMapper.selectCount(
                new LambdaQueryWrapper<CmsNote>()
                        .eq(CmsNote::getStatus, CmsNote.STATUS_REJECTED)
                        .ge(CmsNote::getAuditTime, todayStart)).intValue());

        // 已发布总数 (status=1)
        stats.setPublishedCount(cmsNoteMapper.selectCount(
                new LambdaQueryWrapper<CmsNote>().eq(CmsNote::getStatus, CmsNote.STATUS_PUBLISHED)).intValue());

        // 已屏蔽数量 (status=2)
        stats.setShieldedCount(cmsNoteMapper.selectCount(
                new LambdaQueryWrapper<CmsNote>().eq(CmsNote::getStatus, CmsNote.STATUS_SHIELDED)).intValue());

        stats.setRejectedCount(cmsNoteMapper.selectCount(
                new LambdaQueryWrapper<CmsNote>().eq(CmsNote::getStatus, CmsNote.STATUS_REJECTED)).intValue());

        stats.setUserOffShelfCount(cmsNoteMapper.selectCount(
                new LambdaQueryWrapper<CmsNote>().eq(CmsNote::getStatus, CmsNote.STATUS_USER_OFF_SHELF)).intValue());

        stats.setUserDeletedCount(cmsNoteMapper.selectCount(
                new LambdaQueryWrapper<CmsNote>().eq(CmsNote::getStatus, CmsNote.STATUS_USER_DELETED)).intValue());

        stats.setAdminSoftDeletedCount(cmsNoteMapper.selectCount(
                new LambdaQueryWrapper<CmsNote>().eq(CmsNote::getStatus, CmsNote.STATUS_ADMIN_SOFT_DELETED)).intValue());

        return stats;
    }

    /**
     * 笔记详情
     */
    @Override
    public AdminNoteVO getDetail(Long id) {
        // 查询笔记详情
        CmsNote note = cmsNoteMapper.selectById(id);
        if (note == null) {
            throw AppException.notFound("笔记不存在");
        }
        return toVO(note);
    }

    /**
     * 审核通过
     */
    @Override
    public void approve(Long id) {
        // 查询待审核笔记
        CmsNote note = cmsNoteMapper.selectById(id);
        if (note == null) {
            throw AppException.notFound("笔记不存在");
        }
        if (note.getStatus() != null
                && (note.getStatus() == CmsNote.STATUS_USER_DELETED || note.getStatus() ==
                CmsNote.STATUS_ADMIN_SOFT_DELETED)) {
            throw AppException.badRequest("已删除内容不可审核");
        }
        // 更新笔记审核结果同时刷新笔记列表缓存版本
        note.setStatus(CmsNote.STATUS_PUBLISHED);
        note.setAuditTime(LocalDateTime.now());
        note.setAuditUserId(StpUtil.getLoginIdAsLong());
        note.setRejectReason(null);
        cmsNoteMapper.updateById(note);
        noteRedisSupport.bumpNoteListCacheVersion();
        // 通知作者审核通过
        notificationService.sendToUser(
                note.getUserId(),
                "笔记审核通过",
                "你的笔记《" + StrUtil.blankToDefault(note.getTitle(), "未命名") + "》已通过审核。",
                "note",
                note.getId()
        );
    }

    /**
     * 审核拒绝
     */
    @Override
    public void reject(Long id, String reason) {
        // 查询待处理笔记
        CmsNote note = cmsNoteMapper.selectById(id);
        if (note == null) {
            throw AppException.notFound("笔记不存在");    }
        if (note.getStatus() != null
                && (note.getStatus() == CmsNote.STATUS_USER_DELETED || note.getStatus() == CmsNote.STATUS_ADMIN_SOFT_DELETED)) {
            throw AppException.badRequest("已删除内容不可审核");
        }

        // 更新审核结果和拒绝原因
        note.setStatus(CmsNote.STATUS_REJECTED);
        note.setAuditTime(LocalDateTime.now());
        note.setAuditUserId(StpUtil.getLoginIdAsLong());
        note.setRejectReason(reason);
        cmsNoteMapper.updateById(note);
        noteRedisSupport.bumpNoteListCacheVersion();
        String reasonText = StrUtil.isBlank(reason) ? "未填写" : reason.trim();

        // 通知作者审核未通过
        notificationService.sendToUser(
                note.getUserId(),
                "笔记审核未通过",
                "你的笔记《" + StrUtil.blankToDefault(note.getTitle(),
                        "未命名") + "》未通过审核。\n原因：" + reasonText,
                "note",
                note.getId()
        );
    }

    /**
     * 切换置顶
     */
    @Override
    public boolean toggleSticky(Long id) {
        CmsNote note = cmsNoteMapper.selectById(id);
        if (note == null) {
            throw AppException.notFound("笔记不存在");
        }
        // 切换置顶状态
        boolean newSticky = !Boolean.TRUE.equals(note.getIsSticky());
        note.setIsSticky(newSticky);
        cmsNoteMapper.updateById(note);
        noteRedisSupport.bumpNoteListCacheVersion();
        return newSticky;
    }

    /**
     * 切换推荐
     */
    @Override
    public boolean toggleRecommend(Long id) {
        CmsNote note = cmsNoteMapper.selectById(id);
        if (note == null) {
            throw AppException.notFound("笔记不存在");
        }
        // 切换推荐状态
        boolean newRecommend = !Boolean.TRUE.equals(note.getIsRecommended());
        note.setIsRecommended(newRecommend);
        cmsNoteMapper.updateById(note);
        noteRedisSupport.bumpNoteListCacheVersion();
        return newRecommend;
    }

    /**
     * 切换下架
     */
    @Override
    public boolean toggleShield(Long id, String reason) {
        CmsNote note = cmsNoteMapper.selectById(id);
        if (note == null) {
            throw AppException.notFound("笔记不存在");
        }
        Integer status = note.getStatus();
        if (status == null) {
            throw AppException.badRequest("笔记状态异常");
        }

        // 只允许已通过和已下架的内容做下架或恢复。
        if (status != CmsNote.STATUS_PUBLISHED && status != CmsNote.STATUS_SHIELDED) {
            throw AppException.badRequest("仅已通过的内容才支持下架/恢复");
        }

        // 当前如果已经下架，就恢复为已通过；否则执行下架。
        boolean toDown = status != CmsNote.STATUS_SHIELDED;
        note.setStatus(toDown ? CmsNote.STATUS_SHIELDED : CmsNote.STATUS_PUBLISHED);
        cmsNoteMapper.updateById(note);
        noteRedisSupport.bumpNoteListCacheVersion();

        if (toDown) {
            // 下架时给作者发送原因
            String reasonText = StrUtil.trimToNull(reason);
            String content = "你的笔记《" + StrUtil.blankToDefault(note.getTitle(), "未命名") + "》已被下架，前台暂不可见。";
            if (reasonText != null) {
                content += "\n原因：" + reasonText;
            }
            notificationService.sendToUser(note.getUserId(), "笔记已下架", content, "note", note.getId());
        } else {
            // 恢复上架时通知作者
            notificationService.sendToUser(
                    note.getUserId(),
                    "笔记已恢复上架",
                    "你的笔记《" + StrUtil.blankToDefault(note.getTitle(), "未命名") + "》已恢复上架。",
                    "note",
                    note.getId()
            );
        }

        return toDown;
    }

    /**
     * 软删除笔记
     */
    @Override
    public void softDelete(Long id, String reason) {
        // 查询待删除笔记
        CmsNote note = cmsNoteMapper.selectById(id);
        if (note == null) {
            throw AppException.notFound("笔记不存在");
        }
        if (note.getStatus() != null && note.getStatus() == CmsNote.STATUS_ADMIN_SOFT_DELETED) {
            return;
        }

        note.setStatus(CmsNote.STATUS_ADMIN_SOFT_DELETED);
        note.setIsSticky(false);
        note.setIsRecommended(false);
        note.setAuditTime(LocalDateTime.now());
        note.setAuditUserId(StpUtil.getLoginIdAsLong());
        note.setRejectReason(StrUtil.trimToNull(reason));
        // 这里走逻辑删除，让记录马上从管理端列表里消失。
        note.setIsDeleted(CmsNote.DELETE_DELETED);
        cmsNoteMapper.updateById(note);
        noteRedisSupport.bumpNoteListCacheVersion();

        // 通知作者笔记已删除
        String reasonText = StrUtil.trimToNull(reason);
        String content = "你的笔记《" + StrUtil.blankToDefault(note.getTitle(), "未命名") + "》已被管理员删除。";
        if (reasonText != null) {
            content += "\n原因：" + reasonText;
        }
        notificationService.sendToUser(
                note.getUserId(),
                "笔记已删除",
                content,
                "note",
                note.getId()
        );
    }

    /**
     * 批量操作
     */
    @Override
    public void batchAction(String action, List<Long> ids) {
        // 逐个执行批量动作
        for (Long id : ids) {
            switch (action) {
                case "approve" -> approve(id);
                case "reject" -> reject(id, null);
                case "shield" -> {
                    CmsNote note = cmsNoteMapper.selectById(id);
                    if (note != null && Integer.valueOf(CmsNote.STATUS_PUBLISHED).equals(note.getStatus())) {
                        note.setStatus(CmsNote.STATUS_SHIELDED);
                        cmsNoteMapper.updateById(note);
                        notificationService.sendToUser(
                                note.getUserId(),
                                "笔记已下架",
                                "你的笔记《" + StrUtil.blankToDefault(note.getTitle(), "未命名") + "》已被下架，前台暂不可见。",
                                "note",
                                note.getId()
                        );
                    }
                }
                case "softDelete" -> softDelete(id, null);
                case "delete" -> cmsNoteMapper.deleteById(id);
            }
        }
        noteRedisSupport.bumpNoteListCacheVersion();
    }

    /**
     * 转换后台笔记数据
     */
    private AdminNoteVO toVO(CmsNote note) {
        AdminNoteVO vo = new AdminNoteVO();
        BeanUtil.copyProperties(note, vo);
        // 字段名不同：CmsNote.coverImg -> AdminNoteVO.cover
        vo.setCover(note.getCoverImg());
        vo.setCategory(note.getCategory());
        vo.setTags(note.getTags());

        // 查询作者信息
        SysUser user = sysUserMapper.selectById(note.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setUserAvatar(user.getAvatar());
        }

        // 审核人名称有值时再补充查询。
        if (note.getAuditUserId() != null) {
            SysUser auditor = sysUserMapper.selectById(note.getAuditUserId());
            if (auditor != null) {
                vo.setAuditUserName(StrUtil.blankToDefault(auditor.getNickname(), auditor.getUsername()));
            }
        }

        Integer commentCount = commentMapper.selectCount(
                new LambdaQueryWrapper<CmsComment>()
                        .eq(CmsComment::getNoteId, note.getId())
                        .eq(CmsComment::getStatus, CmsComment.STATUS_NORMAL))
                .intValue();
        vo.setCommentCount(commentCount);

        return vo;
    }
}
