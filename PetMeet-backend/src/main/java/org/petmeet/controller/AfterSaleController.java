package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.dto.AfterSaleApplyDTO;
import org.petmeet.dto.AfterSaleReturnLogisticsDTO;
import org.petmeet.service.OmsAfterSaleService;
import org.petmeet.vo.AfterSaleVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/after-sale")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "After-sale APIs", description = "Apply/view/cancel/complete after-sale")
public class AfterSaleController {

    private final OmsAfterSaleService afterSaleService;

    /**
     * 申请售后
     */
    @PostMapping("/apply")
    @Operation(summary = "Apply after-sale")
    public Result<Long> apply(@Valid @RequestBody AfterSaleApplyDTO dto) {
        // 调用业务层创建售后申请
        Long id = afterSaleService.apply(dto);
        return Result.success("售后申请已提交", id);
    }

    /**
     * 我的售后列表
     */
    @GetMapping("/my/list")
    @Operation(summary = "My after-sale list")
    public Result<Page<AfterSaleVO>> myList(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "Status filter")
            @RequestParam(required = false) Integer status) {
        // 查询当前用户的售后列表
        return Result.success(afterSaleService.pageMy(pageNum, pageSize, status));
    }

    /**
     * 取消售后
     */
    @PostMapping("/cancel/{id}")
    @Operation(summary = "Cancel after-sale request")
    public Result<Void> cancel(@PathVariable Long id) {
        // 调用业务层取消售后申请
        afterSaleService.cancel(id);
        return Result.success("售后申请已取消", null);
    }

    /**
     * 完成售后
     */
    @PostMapping("/complete/{id}")
    @Operation(summary = "Complete after-sale request")
    public Result<Void> complete(@PathVariable Long id) {
        // 调用业务层完成售后流程
        afterSaleService.complete(id);
        return Result.success("售后已确认完成", null);
    }

    @PostMapping("/{id}/return-logistics")
    @Operation(summary = "Submit return logistics")
    public Result<Void> submitReturnLogistics(@PathVariable Long id, @Valid @RequestBody AfterSaleReturnLogisticsDTO dto) {
        afterSaleService.submitReturnLogistics(id, dto);
        return Result.success("退货物流已提交", null);
    }

    /**
     * 删除售后记录
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete my after-sale request")
    public Result<Void> deleteMy(@PathVariable Long id) {
        // 调用业务层删除当前用户的售后记录
        afterSaleService.deleteMy(id);
        return Result.success("售后申请已删除", null);
    }
}
