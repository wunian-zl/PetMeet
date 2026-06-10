package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.service.CmsComplaintService;
import org.petmeet.vo.AdminComplaintVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/complaint")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "Admin Complaint", description = "Complaint management")
public class AdminComplaintController {

    private final CmsComplaintService complaintService;

    /**
     * 投诉列表
     */
    @GetMapping("/list")
    @Operation(summary = "Complaint list")
    public Result<Page<AdminComplaintVO>> list(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "Status") @RequestParam(required = false) Integer status,
            @Parameter(description = "Keyword") @RequestParam(required = false) String keyword) {
        // 调用业务层查询投诉列表
        return Result.success(complaintService.pageAdmin(pageNum, pageSize, status, keyword));
    }

    /**
     * 更新投诉状态
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Update complaint status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String remark) {
        // 调用业务层更新投诉处理状态
        complaintService.updateStatus(id, status, remark);
        return Result.success("Updated", null);
    }

    /**
     * 删除投诉记录
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete complaint")
    public Result<Void> softDelete(@PathVariable Long id) {
        // 调用业务层软删除投诉记录
        complaintService.adminSoftDelete(id);
        return Result.success("Deleted", null);
    }

    /**
     * 批量删除投诉记录
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "Batch soft delete complaints")
    public Result<Void> batchSoftDelete(@RequestBody List<Long> ids) {
        // 调用业务层批量软删除投诉记录
        complaintService.adminBatchSoftDelete(ids);
        return Result.success("Batch deleted", null);
    }
}
