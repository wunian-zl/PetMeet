package org.petmeet.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.petmeet.dto.ComplaintDTO;
import org.petmeet.dto.ComplaintFeedbackDTO;
import org.petmeet.entity.CmsComplaint;
import org.petmeet.entity.CmsNote;
import org.petmeet.entity.SysUser;
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
    private final SysUserMapper userMapper;
    private final SysNotificationService notificationService;

    /**
     * 提交投诉
     */
    @Override
    public Long submitComplaint(ComplaintDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 查询被投诉的笔记
        CmsNote note = noteMapper.selectById(dto.getNoteId());
        if (note == null) {
            throw new RuntimeException("笔记不存在");  }
        if (Objects.equals(note.getUserId(), userId)) {
            throw new RuntimeException("不能投诉自己的笔记"); }
        CmsComplaint latest = complaintMapper.selectOne(new LambdaQueryWrapper<CmsComplaint>()
                .eq(CmsComplaint::getNoteId, dto.getNoteId())
                .eq(CmsComplaint::getUserId, userId)
                .and(w -> w.isNull(CmsComplaint::getUserDeleted)
                        .or().eq(CmsComplaint::getUserDeleted, 0))
                .orderByDesc(CmsComplaint::getCreateTime) .last("limit 1"));
        if (latest != null) {
            if (latest.getStatus() != null && latest.getStatus() == 0) {
                throw new RuntimeException("投诉正在核查中，请耐心等待");}
            if (dto.getParentId() == null) {
                throw new RuntimeException("该笔记已投诉过，请在【通知-投诉】中查看进度与处理结果"); }
            if (!Objects.equals(dto.getParentId(), latest.getId())) {
                throw new RuntimeException("请针对最新的投诉记录进行再次投诉");}
            Integer feedback = latest.getFeedbackStatus() == null ? 0 : latest.getFeedbackStatus();
            if (feedback == 1) {
                throw new RuntimeException("你已反馈满意，无法再次投诉"); }
            if (feedback == 0) {
                // 再次投诉时，默认把上一条投诉记为“不满意”。
                latest.setFeedbackStatus(2);latest.setFeedbackTime(LocalDateTime.now());
                complaintMapper.updateById(latest);}
        } else if (dto.getParentId() != null) {
            throw new RuntimeException("parentId无效");    }
        // 保存新的投诉记录
        CmsComplaint complaint = new CmsComplaint();  complaint.setNoteId(dto.getNoteId());
        complaint.setParentId(dto.getParentId()); complaint.setUserId(userId);
        complaint.setReason(StrUtil.trimToEmpty(dto.getReason()));
        complaint.setContent(StrUtil.trimToNull(dto.getContent()));
        complaint.setStatus(0); complaint.setUserDeleted(0);
        complaint.setFeedbackStatus(0); complaint.setCreateTime(LocalDateTime.now());
        complaintMapper.insert(complaint);
        String noteTitle = note == null ? "相关内容" :
                (StrUtil.blankToDefault(note.getTitle(), "未命名"));
        notificationService.sendToUser(
                userId, "投诉已提交",
                "你对《" + noteTitle + "》的投诉已提交，正在核查中，请耐心等待。\n可在【通知-投诉】查看进度。",
                "complaint",  complaint.getId() );
        return complaint.getId();
    }

    /**
     * 我的投诉列表
     */
    @Override
    public Page<MyComplaintVO> pageMy(Integer pageNum, Integer pageSize, Integer status) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<CmsComplaint> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CmsComplaint> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsComplaint::getUserId, userId);
        wrapper.and(w -> w.isNull(CmsComplaint::getUserDeleted).or().eq(CmsComplaint::getUserDeleted, 0));
        if (status != null) {
            wrapper.eq(CmsComplaint::getStatus, status);
        }
        wrapper.orderByDesc(CmsComplaint::getCreateTime);
        complaintMapper.selectPage(page, wrapper);

        Page<MyComplaintVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<CmsComplaint> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        // 批量查询关联笔记信息
        Set<Long> noteIds = records.stream().map(CmsComplaint::getNoteId).collect(Collectors.toSet());
        List<CmsNote> notes = noteIds.isEmpty() ? Collections.emptyList() : noteMapper.selectBatchIds(noteIds);
        Map<Long, CmsNote> noteMap = notes.stream()
                .collect(Collectors.toMap(CmsNote::getId, n -> n, (a, b) -> a));

        // 组装投诉展示数据
        List<MyComplaintVO> vos = new ArrayList<>();
        for (CmsComplaint c : records) {
            MyComplaintVO vo = new MyComplaintVO();
            vo.setId(c.getId());
            vo.setNoteId(c.getNoteId());
            vo.setParentId(c.getParentId());
            vo.setReason(c.getReason());
            vo.setContent(c.getContent());
            vo.setStatus(c.getStatus());
            vo.setCreateTime(c.getCreateTime());
            vo.setHandleTime(c.getHandleTime());
            vo.setHandleRemark(c.getHandleRemark());
            vo.setFeedbackStatus(c.getFeedbackStatus());
            vo.setFeedbackContent(c.getFeedbackContent());
            vo.setFeedbackTime(c.getFeedbackTime());

            CmsNote n = noteMap.get(c.getNoteId());
            if (n != null) {
                vo.setNoteTitle(n.getTitle());
                vo.setNoteCover(n.getCoverImg());
            }
            vos.add(vo);
        }

        voPage.setRecords(vos);
        return voPage;
    }

    /**
     * 查询某条笔记的最新投诉
     */
    @Override
    public MyComplaintVO getMyLatestByNote(Long noteId) {
        Long userId = StpUtil.getLoginIdAsLong();
        CmsComplaint c = complaintMapper.selectOne(new LambdaQueryWrapper<CmsComplaint>()
                .eq(CmsComplaint::getUserId, userId)
                .eq(CmsComplaint::getNoteId, noteId)
                .and(w -> w.isNull(CmsComplaint::getUserDeleted).or().eq(CmsComplaint::getUserDeleted, 0))
                .orderByDesc(CmsComplaint::getCreateTime)
                .last("limit 1"));
        if (c == null) {
            return null;
        }

        MyComplaintVO vo = new MyComplaintVO();
        vo.setId(c.getId());
        vo.setNoteId(c.getNoteId());
        vo.setParentId(c.getParentId());
        vo.setReason(c.getReason());
        vo.setContent(c.getContent());
        vo.setStatus(c.getStatus());
        vo.setCreateTime(c.getCreateTime());
        vo.setHandleTime(c.getHandleTime());
        vo.setHandleRemark(c.getHandleRemark());
        vo.setFeedbackStatus(c.getFeedbackStatus());
        vo.setFeedbackContent(c.getFeedbackContent());
        vo.setFeedbackTime(c.getFeedbackTime());

        CmsNote note = noteMapper.selectById(noteId);
        if (note != null) {
            vo.setNoteTitle(note.getTitle());
            vo.setNoteCover(note.getCoverImg());
        }
        return vo;
    }

    /**
     * 我的投诉详情
     */
    @Override
    public MyComplaintVO getMyDetail(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        CmsComplaint c = complaintMapper.selectById(id);
        if (c == null || Integer.valueOf(1).equals(c.getUserDeleted())) {
            throw new RuntimeException("投诉不存在"); }
               if (!Objects.equals(c.getUserId(), userId)) {
            throw new RuntimeException("无权限");  }
        MyComplaintVO vo = new MyComplaintVO();
        vo.setId(c.getId());
        vo.setNoteId(c.getNoteId());
        vo.setParentId(c.getParentId());
        vo.setReason(c.getReason());
        vo.setContent(c.getContent());
        vo.setStatus(c.getStatus());
        vo.setCreateTime(c.getCreateTime());
        vo.setHandleTime(c.getHandleTime());
        vo.setHandleRemark(c.getHandleRemark());
        vo.setFeedbackStatus(c.getFeedbackStatus());
        vo.setFeedbackContent(c.getFeedbackContent());
        vo.setFeedbackTime(c.getFeedbackTime());
        CmsNote note = noteMapper.selectById(c.getNoteId());
        if (note != null) {
            vo.setNoteTitle(note.getTitle());
            vo.setNoteCover(note.getCoverImg());}
        return vo;  }

    /**
     * 反馈投诉结果
     */
    @Override
    public void feedback(Long id, ComplaintFeedbackDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        CmsComplaint complaint = complaintMapper.selectById(id);
        if (complaint == null || Integer.valueOf(1).equals(complaint.getUserDeleted())) {
            throw new RuntimeException("投诉不存在");
        }
        if (!Objects.equals(complaint.getUserId(), userId)) {
            throw new RuntimeException("无权限");
        }
        if (complaint.getStatus() == null || complaint.getStatus() == 0) {
            throw new RuntimeException("投诉正在核查中，暂不能反馈");
        }

        Integer current = complaint.getFeedbackStatus() == null ? 0 : complaint.getFeedbackStatus();
        if (current != 0) {
            throw new RuntimeException("已反馈过处理结果");
        }

        // 保存用户对处理结果的反馈
        complaint.setFeedbackStatus(dto.getFeedbackStatus());
        complaint.setFeedbackContent(StrUtil.trimToNull(dto.getContent()));
        complaint.setFeedbackTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
    }

    /**
     * 删除我的投诉
     */
    @Override
    public void deleteMyComplaint(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        CmsComplaint complaint = complaintMapper.selectById(id);
        if (complaint == null || Integer.valueOf(1).equals(complaint.getUserDeleted())) {
            throw new RuntimeException("投诉不存在");
        }
        if (!Objects.equals(complaint.getUserId(), userId)) {
            throw new RuntimeException("无权限");
        }
        if (complaint.getStatus() == null || complaint.getStatus() == 0) {
            throw new RuntimeException("投诉核查中，暂不能删除");
        }
        complaint.setUserDeleted(1);
        complaintMapper.updateById(complaint);
    }

    /**
     * 后台投诉列表
     */
    @Override
    public Page<AdminComplaintVO> pageAdmin(Integer pageNum, Integer pageSize, Integer status, String keyword) {
        Page<CmsComplaint> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CmsComplaint> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNull(CmsComplaint::getAdminDeleted).or().eq(CmsComplaint::getAdminDeleted, 0));
        if (status != null) {
            wrapper.eq(CmsComplaint::getStatus, status);
        }

        if (StrUtil.isNotBlank(keyword)) {
            String k = keyword.trim();
            List<Long> noteIds = noteMapper.selectList(new LambdaQueryWrapper<CmsNote>()
                            .select(CmsNote::getId)
                            .like(CmsNote::getTitle, k))
                    .stream().map(CmsNote::getId).toList();
            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .select(SysUser::getId)
                            .and(w -> w.like(SysUser::getUsername, k).or().like(SysUser::getNickname, k)))
                    .stream().map(SysUser::getId).toList();

            wrapper.and(w -> {
                boolean hasCondition = false;
                if (!noteIds.isEmpty()) {
                    w.in(CmsComplaint::getNoteId, noteIds);
                    hasCondition = true;
                }
                if (!userIds.isEmpty()) {
                    if (hasCondition) {
                        w.or();
                    }
                    w.in(CmsComplaint::getUserId, userIds);
                    hasCondition = true;
                }
                if (!hasCondition) {
                    w.eq(CmsComplaint::getId, -1);
                }
            });
        }

        wrapper.orderByDesc(CmsComplaint::getCreateTime);
        complaintMapper.selectPage(page, wrapper);

        List<CmsComplaint> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        }

        Set<Long> noteIds = records.stream().map(CmsComplaint::getNoteId).collect(Collectors.toSet());
        Set<Long> reporterIds = records.stream().map(CmsComplaint::getUserId).collect(Collectors.toSet());

        List<CmsNote> notes = noteIds.isEmpty() ? Collections.emptyList() : noteMapper.selectBatchIds(noteIds);
        Map<Long, CmsNote> noteMap = notes.stream()
                .collect(Collectors.toMap(CmsNote::getId, n -> n, (a, b) -> a));

        Set<Long> authorIds = notes.stream().map(CmsNote::getUserId).collect(Collectors.toSet());
        Set<Long> userIds = new HashSet<>(reporterIds);
        userIds.addAll(authorIds);
        List<SysUser> users = userIds.isEmpty() ? Collections.emptyList() : userMapper.selectBatchIds(userIds);
        Map<Long, SysUser> userMap = users.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        // 组装后台投诉展示数据
        List<AdminComplaintVO> vos = new ArrayList<>();
        for (CmsComplaint complaint : records) {
            AdminComplaintVO vo = new AdminComplaintVO();
            vo.setId(complaint.getId());
            vo.setNoteId(complaint.getNoteId());
            vo.setParentId(complaint.getParentId());
            vo.setReason(complaint.getReason());
            vo.setContent(complaint.getContent());
            vo.setStatus(complaint.getStatus());
            vo.setHandleRemark(complaint.getHandleRemark());
            vo.setFeedbackStatus(complaint.getFeedbackStatus());
            vo.setFeedbackContent(complaint.getFeedbackContent());
            vo.setFeedbackTime(complaint.getFeedbackTime());
            vo.setCreateTime(complaint.getCreateTime());
            vo.setHandleTime(complaint.getHandleTime());
            vo.setHandlerId(complaint.getHandlerId());

            CmsNote note = noteMap.get(complaint.getNoteId());
            if (note != null) {
                vo.setNoteTitle(note.getTitle());
                vo.setNoteAuthorId(note.getUserId());
                SysUser author = userMap.get(note.getUserId());
                if (author != null) {
                    vo.setNoteAuthorName(StrUtil.blankToDefault(author.getNickname(), author.getUsername()));
                }
            }

            SysUser reporter = userMap.get(complaint.getUserId());
            if (reporter != null) {
                vo.setReporterId(reporter.getId());
                vo.setReporterName(StrUtil.blankToDefault(reporter.getNickname(), reporter.getUsername()));
                vo.setReporterAvatar(reporter.getAvatar());
            }
            vos.add(vo);
        }

        Page<AdminComplaintVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    /**
     * 更新投诉状态
     */
    @Override
    public void updateStatus(Long id, Integer status, String remark) {
        CmsComplaint complaint = complaintMapper.selectById(id);
        if (complaint == null || Integer.valueOf(1).equals(complaint.getAdminDeleted())) {
            throw new RuntimeException("投诉不存在");}
        complaint.setStatus(status);
        complaint.setHandleRemark(StrUtil.trimToNull(remark));
        complaint.setHandleTime(LocalDateTime.now());
        complaint.setHandlerId(StpUtil.getLoginIdAsLong());
        complaintMapper.updateById(complaint);
        // 把处理结果通知给投诉人
        CmsNote note = noteMapper.selectById(complaint.getNoteId());
        String noteTitle = note == null ? "相关内容" : (StrUtil.blankToDefault(note.getTitle(),
                "未命名"));
        if (status != null) {
            if (status == 1) {
                String remarkText = StrUtil.trimToNull(remark);
                String extra = remarkText == null ? "" : ("\n处理说明：" + remarkText);
                notificationService.sendToUser(
                        complaint.getUserId(),
                        "投诉已处理",
                        "你对《" + noteTitle + "》的投诉已处理完成，感谢反馈。" +
                                extra + "\n请在【通知-投诉】中反馈是否满意。",
                        "complaint",
                        complaint.getId()  );
            } else if (status == 2) {
                String remarkText = StrUtil.trimToNull(remark);
                String extra = remarkText == null ? "" : ("\n说明：" + remarkText);
                notificationService.sendToUser(
                        complaint.getUserId(),
                        "投诉被驳回",
                        "你对《" + noteTitle + "》的投诉已被驳回。如有更多证据可再次反馈。" + extra +
                                "\n可在【通知-投诉】中选择不满意并再次投诉。",
                        "complaint",
                        complaint.getId() ); } }   }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminSoftDelete(Long id) {
        CmsComplaint complaint = complaintMapper.selectById(id);
        if (complaint == null || Integer.valueOf(1).equals(complaint.getAdminDeleted())) {
            throw new RuntimeException("投诉不存在"); }
        if (!canAdminSoftDelete(complaint.getStatus())) {
            throw new RuntimeException("仅已处理或已驳回投诉可删除");}
        complaintMapper.update(null, new LambdaUpdateWrapper<CmsComplaint>()
                .eq(CmsComplaint::getId, id)
                .and(w -> w.isNull(CmsComplaint::getAdminDeleted).or()
                        .eq(CmsComplaint::getAdminDeleted, 0))
                .set(CmsComplaint::getAdminDeleted, 1));
    }

    /**
     * 后台批量软删除投诉
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminBatchSoftDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Long> normalizedIds = ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        // 逐个复用单删逻辑
        for (Long id : normalizedIds) {
            adminSoftDelete(id);
        }
    }

    /**
     * 判断是否允许后台删除
     */
    private boolean canAdminSoftDelete(Integer status) {
        return status != null && (status == 1 || status == 2);
    }
}
