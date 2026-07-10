package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.dto.AdminAfterSaleActionDTO;
import org.petmeet.service.AdminAfterSaleService;
import org.petmeet.vo.AdminAfterSaleVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/after-sale")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "Admin after-sale APIs", description = "List and handle after-sale requests")
public class AdminAfterSaleController {

    private final AdminAfterSaleService adminAfterSaleService;

    /**
     * 售后列表
     */
    @GetMapping("/list")
    @Operation(summary = "After-sale list")
    public Result<Page<AdminAfterSaleVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "Status: 0 pending, 1 processing, 2 done, 3 rejected, 4 canceled, 5 waiting buyer return, 6 waiting merchant receive, 7 refunding, 8 exchange shipped")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "Type: 0 refund only, 1 return refund, 2 exchange")
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String keyword) {
        // 调用业务层查询售后列表
        return Result.success(adminAfterSaleService.pageList(pageNum, pageSize, status, type, keyword));
    }

    @GetMapping("/{id}")
    @Operation(summary = "After-sale detail")
    public Result<AdminAfterSaleVO> detail(@PathVariable Long id) {
        return Result.success(adminAfterSaleService.detail(id));
    }

    /**
     * 更新售后状态
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Update after-sale status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String remark) {
        // 调用业务层更新售后状态
        adminAfterSaleService.updateStatus(id, status, remark);
        return Result.success("状态已更新", null);
    }

    @PostMapping("/{id}/approve-refund")
    @Operation(summary = "Approve refund-only after-sale")
    public Result<Void> approveRefund(@PathVariable Long id, @Valid @RequestBody(required = false) AdminAfterSaleActionDTO dto) {
        adminAfterSaleService.approveRefund(id, dto);
        return Result.success("退款已处理", null);
    }

    @PostMapping("/{id}/approve-return")
    @Operation(summary = "Approve return for return-refund or exchange")
    public Result<Void> approveReturn(@PathVariable Long id, @Valid @RequestBody AdminAfterSaleActionDTO dto) {
        adminAfterSaleService.approveReturn(id, dto);
        return Result.success("已同意退货", null);
    }

    @PostMapping("/{id}/confirm-return-refund")
    @Operation(summary = "Confirm return received and refund")
    public Result<Void> confirmReturnRefund(@PathVariable Long id, @Valid @RequestBody(required = false) AdminAfterSaleActionDTO dto) {
        adminAfterSaleService.confirmReturnRefund(id, dto);
        return Result.success("已确认收货并退款", null);
    }

    @PostMapping("/{id}/confirm-return-exchange")
    @Operation(summary = "Confirm return received for exchange")
    public Result<Void> confirmReturnExchange(@PathVariable Long id, @Valid @RequestBody(required = false) AdminAfterSaleActionDTO dto) {
        adminAfterSaleService.confirmReturnExchange(id, dto);
        return Result.success("已确认收货", null);
    }

    @PostMapping("/{id}/ship-exchange")
    @Operation(summary = "Ship exchange goods")
    public Result<Void> shipExchange(@PathVariable Long id, @Valid @RequestBody AdminAfterSaleActionDTO dto) {
        adminAfterSaleService.shipExchange(id, dto);
        return Result.success("换货已发货", null);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject after-sale")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody(required = false) AdminAfterSaleActionDTO dto) {
        adminAfterSaleService.reject(id, dto);
        return Result.success("售后已拒绝", null);
    }

    /**
     * 删除售后记录
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete after-sale record")
    public Result<Void> softDelete(@PathVariable Long id) {
        // 调用业务层软删除售后记录
        adminAfterSaleService.softDelete(id);
        return Result.success("删除成功", null);
    }

    /**
     * 批量删除售后记录
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "Batch soft delete after-sale records")
    public Result<Void> batchSoftDelete(@RequestBody List<Long> ids) {
        // 调用业务层批量软删除售后记录
        adminAfterSaleService.batchSoftDelete(ids);
        return Result.success("批量删除成功", null);
    }
}
