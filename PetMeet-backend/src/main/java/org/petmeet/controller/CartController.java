package org.petmeet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.petmeet.common.Result;
import org.petmeet.dto.CartAddDTO;
import org.petmeet.service.OmsCartItemService;
import org.petmeet.vo.CartItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车控制器
 * @author zjx
 */
@Tag(name = "购物车", description = "购物车管理接口")
@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private OmsCartItemService cartItemService;

    /**
     * 购物车列表
     */
    @Operation(summary = "获取购物车列表")
    @GetMapping("/list")
    public Result<List<CartItemVO>> list() {
        // 查询当前用户的购物车列表
        List<CartItemVO> list = cartItemService.getCartList();
        return Result.success(list);
    }

    /**
     * 添加到购物车
     */
    @Operation(summary = "添加商品到购物车")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody CartAddDTO dto) {
        // 调用业务层把商品加入购物车
        cartItemService.addToCart(dto);
        return Result.success();
    }

    /**
     * 修改购物车数量
     */
    @Operation(summary = "更新购物车商品数量")
    @PutMapping("/update")
    public Result<Void> update(
            @Parameter(description = "购物车项ID") @RequestParam Long cartItemId,
            @Parameter(description = "数量") @RequestParam Integer quantity) {
        // 调用业务层修改购物车商品数量
        cartItemService.updateQuantity(cartItemId, quantity);
        return Result.success();
    }

    /**
     * 删除购物车商品
     */
    @Operation(summary = "删除购物车商品")
    @DeleteMapping("/delete/{cartItemId}")
    public Result<Void> delete(
            @Parameter(description = "购物车项ID") @PathVariable Long cartItemId) {
        // 调用业务层删除购物车商品
        cartItemService.removeFromCart(cartItemId);
        return Result.success();
    }

    /**
     * 清空购物车
     */
    @Operation(summary = "清空购物车")
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        // 调用业务层清空当前用户购物车
        cartItemService.clearCart();
        return Result.success();
    }

    /**
     * 选中购物车商品
     */
    @Operation(summary = "选中/取消选中购物车商品")
    @PutMapping("/select")
    public Result<Void> select(
            @Parameter(description = "购物车项ID") @RequestParam Long cartItemId,
            @Parameter(description = "是否选中") @RequestParam Boolean selected) {
        // 调用业务层更新选中状态
        cartItemService.updateSelected(cartItemId, selected);
        return Result.success();
    }

    /**
     * 全选购物车商品
     */
    @Operation(summary = "全选/取消全选")
    @PutMapping("/selectAll")
    public Result<Void> selectAll(
            @Parameter(description = "是否全选") @RequestParam Boolean selected) {
        // 调用业务层更新全选状态
        cartItemService.selectAll(selected);
        return Result.success();
    }

    /**
     * 购物车数量
     */
    @Operation(summary = "获取购物车商品数量")
    @GetMapping("/count")
    public Result<Integer> count() {
        // 查询购物车商品数量
        Integer count = cartItemService.getCartCount();
        return Result.success(count);
    }
}
