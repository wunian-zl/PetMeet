package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.dto.AdminProductSaveDTO;
import org.petmeet.service.AdminProductService;
import org.petmeet.vo.AdminProductDetailVO;
import org.petmeet.vo.AdminProductVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端商品管理控制器
 *
 * @author zjx
 */
@RestController
@RequestMapping("/admin/product")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "管理端商品接口", description = "商品管理CRUD、上下架、批量操作")
public class AdminProductController {

    private final AdminProductService adminProductService;

    /**
     * 商品列表
     */
    @GetMapping("/list")
    @Operation(summary = "商品列表", description = "分页查询商品，支持分类、状态筛选")
    public Result<Page<AdminProductVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "商品状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        // 查询商品分页数据
        return Result.success(adminProductService.pageList(pageNum, pageSize, categoryId, status, keyword));
    }

    /**
     * 商品详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "商品详情")
    public Result<AdminProductDetailVO> detail(@PathVariable Long id) {
        // 查询指定商品详情
        return Result.success(adminProductService.getDetail(id));
    }

    /**
     * 新增商品
     */
    @PostMapping
    @Operation(summary = "新增商品")
    public Result<Long> create(@RequestBody AdminProductSaveDTO product) {
        // 调用业务层创建商品
        Long id = adminProductService.createProduct(product);
        return Result.success("创建成功", id);
    }

    /**
     * 更新商品
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新商品")
    public Result<Void> update(@PathVariable Long id, @RequestBody AdminProductSaveDTO product) {
        // 先补齐商品 id，再调用业务层更新
        product.setId(id);
        adminProductService.updateProduct(product);
        return Result.success("更新成功", null);
    }

    /**
     * 修改商品状态
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "上架/下架商品")
    public Result<Void> changeStatus(
            @PathVariable Long id,
            @Parameter(description = "状态: 0下架/1上架") @RequestParam Integer status) {
        // 调用业务层切换商品上下架状态
        adminProductService.changeStatus(id, status);
        return Result.success(status == 1 ? "已上架" : "已下架", null);
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品")
    public Result<Void> delete(@PathVariable Long id) {
        // 调用业务层删除商品
        adminProductService.deleteProduct(id);
        return Result.success("删除成功", null);
    }

    /**
     * 批量操作商品
     */
    @PostMapping("/batch")
    @Operation(summary = "批量操作")
    public Result<Void> batch(
            @Parameter(description = "操作类型: online/offline/delete") @RequestParam String action,
            @RequestBody List<Long> ids) {
        // 调用业务层执行批量操作
        adminProductService.batchAction(action, ids);
        return Result.success("批量操作完成", null);
    }

}
