package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.service.AdminOrderService;
import org.petmeet.vo.AdminOrderVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/order")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "Admin order APIs", description = "List/detail/ship/refund/cancel orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    /**
     * 订单列表
     */
    @GetMapping("/list")
    @Operation(summary = "Order list")
    public Result<Page<AdminOrderVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "订单状态")
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        // 查询管理端订单列表
        return Result.success(adminOrderService.pageList(pageNum, pageSize, status, orderNo, startTime, endTime));
    }

    /**
     * 订单详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "Order detail")
    public Result<AdminOrderVO> detail(@PathVariable Long id) {
        // 查询指定订单详情
        return Result.success(adminOrderService.getDetail(id));
    }

    /**
     * 发货
     */
    @PutMapping("/{id}/ship")
    @Operation(summary = "Ship order")
    public Result<Void> ship(@PathVariable Long id, @RequestBody Map<String, String> shipInfo) {
        // 调用业务层处理订单发货
        adminOrderService.ship(id, shipInfo.get("company"), shipInfo.get("trackingNo"));
        return Result.success("发货成功", null);
    }

    /**
     * 处理退款
     */
    @PutMapping("/{id}/refund")
    @Operation(summary = "Handle refund for refunding orders")
    public Result<Void> refund(@PathVariable Long id, @RequestBody Map<String, Object> refundInfo) {
        // 调用业务层处理退款结果
        adminOrderService.refund(id, refundInfo);
        return Result.success("退款处理完成", null);
    }

    /**
     * 取消订单
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel order")
    public Result<Void> cancel(@PathVariable Long id) {
        // 调用业务层取消订单
        adminOrderService.cancel(id);
        return Result.success("订单已取消", null);
    }

    /**
     * 修改收货地址
     */
    @PutMapping("/{id}/address")
    @Operation(summary = "Update receiver address")
    public Result<Void> updateAddress(@PathVariable Long id, @RequestBody Map<String, String> addressInfo) {
        // 调用业务层更新订单地址
        adminOrderService.updateAddress(id, addressInfo);
        return Result.success("地址已更新", null);
    }

    /**
     * 导出订单
     */
    @GetMapping("/export")
    @Operation(summary = "Export orders")
    public Result<String> export(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        // 调用业务层生成导出结果
        return Result.success("导出任务已创建", adminOrderService.export(status, startTime, endTime));
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete order")
    public Result<Void> softDelete(@PathVariable Long id) {
        // 调用业务层软删除订单
        adminOrderService.softDelete(id);
        return Result.success("删除成功", null);
    }

    /**
     * 批量删除订单
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "Batch soft delete orders")
    public Result<Void> batchSoftDelete(@RequestBody List<Long> ids) {
        // 调用业务层批量软删除订单
        adminOrderService.batchSoftDelete(ids);
        return Result.success("批量删除成功", null);
    }
}
