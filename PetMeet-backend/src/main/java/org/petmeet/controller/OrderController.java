package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.dto.OrderReviewDTO;
import org.petmeet.dto.OrderSubmitDTO;
import org.petmeet.service.OmsOrderService;
import org.petmeet.vo.OrderDetailVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "Order APIs", description = "Submit/pay/cancel/confirm/review orders")
public class OrderController {

    private final OmsOrderService orderService;

    /**
     * 提交订单
     */
    @PostMapping("/submit")
    @Operation(summary = "Submit order")
    public Result<Long> submitOrder(@Valid @RequestBody OrderSubmitDTO dto) {
        // 调用业务层创建订单
        Long orderId = orderService.submitOrder(dto);
        return Result.success("Order submitted", orderId);
    }

    /**
     * 订单详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "Order detail")
    public Result<OrderDetailVO> getOrderDetail(
            @Parameter(description = "Order id") @PathVariable Long id) {
        // 查询当前用户订单详情
        return Result.success(orderService.getOrderDetail(id));
    }

    /**
     * 我的订单
     */
    @GetMapping("/list")
    @Operation(summary = "My orders")
    public Result<Page<OrderDetailVO>> listMyOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "Order status: 0 pending-pay, 1 paid, 2 shipped, 3 completed, 4 closed, 5 refunding")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "Review status: 0 pending, 1 reviewed")
            @RequestParam(required = false) Integer reviewStatus) {
        // 查询当前用户订单列表
        return Result.success(orderService.pageMyOrders(pageNum, pageSize, status, reviewStatus));
    }

    /**
     * 模拟支付
     */
    @PostMapping("/pay/{id}")
    @Operation(summary = "Mock pay")
    public Result<Void> pay(@PathVariable Long id) {
        // 调用业务层把订单改成已支付
        orderService.pay(id);
        return Result.success("Payment success", null);
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel/{id}")
    @Operation(summary = "Cancel order")
    public Result<Void> cancel(@PathVariable Long id) {
        // 调用业务层取消订单
        orderService.cancel(id);
        return Result.success("Order cancel request accepted", null);
    }

    /**
     * 确认收货
     */
    @PostMapping("/confirm/{id}")
    @Operation(summary = "Confirm receipt")
    public Result<Void> confirm(@PathVariable Long id) {
        // 调用业务层确认收货
        orderService.confirmReceipt(id);
        return Result.success("Receipt confirmed", null);
    }

    /**
     * 订单评价
     */
    @PostMapping("/review/{id}")
    @Operation(summary = "Submit order review")
    public Result<Void> review(@PathVariable Long id, @Valid @RequestBody OrderReviewDTO dto) {
        // 调用业务层提交订单评价
        orderService.review(id, dto);
        return Result.success("Review submitted", null);
    }

    /**
     * 删除评价
     */
    @DeleteMapping("/review/{id}")
    @Operation(summary = "Delete my order review")
    public Result<Void> deleteReview(@PathVariable Long id) {
        // 调用业务层删除订单评价
        orderService.deleteReview(id);
        return Result.success("Review deleted", null);
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete my order")
    public Result<Void> deleteMyOrder(@PathVariable Long id) {
        // 调用业务层删除当前用户订单
        orderService.deleteMyOrder(id);
        return Result.success("Order deleted", null);
    }

    /**
     * 批量删除订单
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "Batch delete my orders")
    public Result<Void> batchDeleteMyOrders(@RequestBody List<Long> orderIds) {
        // 调用业务层批量删除订单
        orderService.batchDeleteMyOrders(orderIds);
        return Result.success("Orders deleted", null);
    }
}
