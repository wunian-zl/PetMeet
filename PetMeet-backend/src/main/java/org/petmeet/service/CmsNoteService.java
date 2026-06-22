package org.petmeet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.petmeet.dto.NotePublishDTO;
import org.petmeet.entity.CmsNote;
import org.petmeet.vo.NoteDetailVO;
import org.petmeet.vo.NoteListVO;

public interface CmsNoteService extends IService<CmsNote> {
    Long publish(NotePublishDTO dto);

    NoteDetailVO getDetail(Long noteId);

    Page<NoteListVO> pageList(Integer pageNum, Integer pageSize, String keyword, Long productId, String category, Boolean recommended, String tag);

    Page<NoteListVO> pageMyNotes(Integer pageNum, Integer pageSize);

    Page<NoteListVO> pageMyCollectedNotes(Integer pageNum, Integer pageSize);

    Page<NoteListVO> pageMyLikedNotes(Integer pageNum, Integer pageSize);

    boolean toggleLike(Long noteId);

    boolean toggleRecommend(Long noteId);

    boolean toggleCollect(Long noteId);

    boolean toggleMyShelf(Long noteId);

    void deleteMyNote(Long noteId);

    void syncLikeCountToDb();
}
