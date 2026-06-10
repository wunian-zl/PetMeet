package org.petmeet.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.petmeet.dto.CommentCreateDTO;
import org.petmeet.entity.CmsComment;
import org.petmeet.entity.CmsNote;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.CmsCommentMapper;
import org.petmeet.mapper.CmsNoteMapper;
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

    private final CmsNoteMapper cmsNoteMapper;
    private final SysUserService sysUserService;

    /**
     * 评论列表
     */
    @Override
    public Page<CommentVO> pageList(Long noteId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<CmsComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsComment::getNoteId, noteId).orderByAsc(CmsComment::getCreateTime);

        Page<CmsComment> page = new Page<>(pageNum, pageSize);
        this.page(page, wrapper);

        Page<CommentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());

        List<CmsComment> records = page.getRecords();
        if (records.isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        // 批量查询评论用户信息
        Set<Long> userIds = records.stream().map(CmsComment::getUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = sysUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        Long currentUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;

        List<CommentVO> voList = records.stream().map(comment -> {
            CommentVO vo = new CommentVO();
            BeanUtil.copyProperties(comment, vo);

            // 填充评论人昵称和头像
            SysUser user = userMap.get(comment.getUserId());
            if (user != null) {
                vo.setUserNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
                vo.setUserAvatar(user.getAvatar());
            } else {
                vo.setUserNickname("已注销用户");
                vo.setUserAvatar("");
            }

            vo.setMine(currentUserId != null && currentUserId.equals(comment.getUserId()));
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 新增评论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(CommentCreateDTO dto) {
        // 校验笔记是否存在且已发布
        CmsNote note = cmsNoteMapper.selectById(dto.getNoteId());
        if (note == null || note.getStatus() == null || note.getStatus() != 1) {
            throw new RuntimeException("笔记不存在或未发布");
        }

        Long userId = StpUtil.getLoginIdAsLong();

        // 保存评论内容
        CmsComment comment = new CmsComment();
        comment.setNoteId(dto.getNoteId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent().trim());
        comment.setCreateTime(LocalDateTime.now());
        this.save(comment);
        return comment.getId();
    }

    /**
     * 删除评论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id) {
        // 查询评论
        CmsComment comment = this.getById(id);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        SysUser currentUser = sysUserService.getById(userId);
        boolean isAdmin = currentUser != null && "admin".equalsIgnoreCase(currentUser.getRole());
        if (!isAdmin && !userId.equals(comment.getUserId())) {
            throw new RuntimeException("无权限删除评论");
        }

        // 只允许管理员或评论作者删除
        this.removeById(id);
    }
}
