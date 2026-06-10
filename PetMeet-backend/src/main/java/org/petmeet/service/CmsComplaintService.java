package org.petmeet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.petmeet.dto.ComplaintDTO;
import org.petmeet.dto.ComplaintFeedbackDTO;
import org.petmeet.entity.CmsComplaint;
import org.petmeet.vo.AdminComplaintVO;
import org.petmeet.vo.MyComplaintVO;

import java.util.List;

public interface CmsComplaintService extends IService<CmsComplaint> {

    Long submitComplaint(ComplaintDTO dto);

    Page<MyComplaintVO> pageMy(Integer pageNum, Integer pageSize, Integer status);

    MyComplaintVO getMyLatestByNote(Long noteId);

    MyComplaintVO getMyDetail(Long id);

    void feedback(Long id, ComplaintFeedbackDTO dto);

    void deleteMyComplaint(Long id);

    Page<AdminComplaintVO> pageAdmin(Integer pageNum, Integer pageSize, Integer status, String keyword);

    void updateStatus(Long id, Integer status, String remark);

    void adminSoftDelete(Long id);

    void adminBatchSoftDelete(List<Long> ids);
}
