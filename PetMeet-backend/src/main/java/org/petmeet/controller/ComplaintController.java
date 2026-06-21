package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.dto.ComplaintDTO;
import org.petmeet.dto.ComplaintFeedbackDTO;
import org.petmeet.service.CmsComplaintService;
import org.petmeet.vo.MyComplaintVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/complaint")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "Complaint", description = "Note complaint")
public class ComplaintController {

    private final CmsComplaintService complaintService;

    /**
     * 提交投诉
     */
    @PostMapping
    @Operation(summary = "Submit complaint")
    public Result<Long> submit(@Valid @RequestBody ComplaintDTO dto) {
        // 调用业务层提交投诉
        Long id = complaintService.submitComplaint(dto);
        return Result.success("提交成功", id);
    }

    /**
     * 我的投诉列表
     */
    @GetMapping("/my/list")
    @Operation(summary = "My complaint list")
    public Result<Page<MyComplaintVO>> myList(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "Status: 0=pending,1=handled,2=rejected") @RequestParam(required = false) Integer status) {
        // 查询当前用户的投诉列表
        return Result.success(complaintService.pageMy(pageNum, pageSize, status));
    }

    /**
     * 最近一次投诉
     */
    @GetMapping("/my/latest")
    @Operation(summary = "My latest complaint for a note")
    public Result<MyComplaintVO> myLatest(@RequestParam Long noteId) {
        // 查询当前用户对某篇笔记最近一次投诉
        return Result.success(complaintService.getMyLatestByNote(noteId));
    }

    /**
     * 投诉详情
     */
    @GetMapping("/my/{id}")
    @Operation(summary = "My complaint detail")
    public Result<MyComplaintVO> myDetail(@PathVariable Long id) {
        // 查询当前用户的投诉详情
        return Result.success(complaintService.getMyDetail(id));
    }

    /**
     * 删除投诉
     */
    @DeleteMapping("/my/{id}")
    @Operation(summary = "Delete my complaint")
    public Result<Void> deleteMy(@PathVariable Long id) {
        // 调用业务层删除当前用户投诉
        complaintService.deleteMyComplaint(id);
        return Result.success("删除成功", null);
    }

    /**
     * 投诉反馈
     */
    @PutMapping("/{id}/feedback")
    @Operation(summary = "Feedback for complaint result")
    public Result<Void> feedback(@PathVariable Long id, @Valid @RequestBody ComplaintFeedbackDTO dto) {
        // 调用业务层提交投诉反馈
        complaintService.feedback(id, dto);
        return Result.success("操作成功", null);
    }
}
