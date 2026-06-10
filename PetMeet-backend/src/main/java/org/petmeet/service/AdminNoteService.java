package org.petmeet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.petmeet.vo.AdminNoteVO;
import org.petmeet.vo.AdminNoteStatsVO;

import java.util.List;

/**
 * 管理端笔记服务接口
 */
public interface AdminNoteService {

    Page<AdminNoteVO> pageList(Integer pageNum, Integer pageSize, Integer status, String keyword, String category, String tag);

    AdminNoteStatsVO getStats();

    AdminNoteVO getDetail(Long id);

    void approve(Long id);

    void reject(Long id, String reason);

    boolean toggleSticky(Long id);

    boolean toggleRecommend(Long id);

    boolean toggleShield(Long id, String reason);

    void softDelete(Long id, String reason);

    void batchAction(String action, List<Long> ids);
}
