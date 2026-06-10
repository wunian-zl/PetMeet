package org.petmeet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.petmeet.dto.CommentCreateDTO;
import org.petmeet.entity.CmsComment;
import org.petmeet.vo.CommentVO;

public interface CmsCommentService extends IService<CmsComment> {
    Page<CommentVO> pageList(Long noteId, Integer pageNum, Integer pageSize);

    Long addComment(CommentCreateDTO dto);

    void deleteComment(Long id);
}
