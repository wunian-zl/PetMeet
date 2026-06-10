package org.petmeet.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.entity.PmsCategory;
import org.petmeet.service.PmsCategoryService;
import org.petmeet.service.PmsProductService;
import org.petmeet.vo.ProductDetailVO;
import org.petmeet.vo.ProductListVO;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
@Tag(name = "商品接口", description = "商品分类、列表、详情")
public class ProductController {

    private final PmsCategoryService pmsCategoryService;
    private final PmsProductService pmsProductService;

    /**
     * 商品分类列表
     */
    @GetMapping("/category/list")
    @Operation(summary = "获取商品分类列表")
    public Result<List<PmsCategory>> listCategory() {
        // 查询启用中的商品分类
        return Result.success(pmsCategoryService.listEnabled());
    }

    /**
     * 商品列表
     */
    @GetMapping("/list")
    @Operation(summary = "商品列表", description = "分页查询商品，可按分类和关键词筛选")
    public Result<Page<ProductListVO>> listProducts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "分类ID列表（逗号分隔）") @RequestParam(required = false) String categoryIds,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "上架时间范围（近N天）") @RequestParam(required = false) Integer recentDays) {
        // 把前端传来的分类字符串拆成分类 id 列表
        List<Long> idList = new ArrayList<>();
        if (categoryIds != null && !categoryIds.trim().isEmpty()) {
            String[] parts = categoryIds.split(",");
            for (String p : parts) {
                if (p == null) continue;
                String s = p.trim();
                if (s.isEmpty()) continue;
                try {
                    idList.add(Long.parseLong(s));
                } catch (NumberFormatException ignored) {
                }
            }
        } else if (categoryId != null) {
            idList.add(categoryId);
        }

        // 调用业务层查询商品分页数据
        return Result.success(pmsProductService.pageList(pageNum, pageSize, idList, keyword, recentDays));
    }

    /**
     * 商品详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "商品详情")
    public Result<ProductDetailVO> getDetail(@PathVariable Long id) {
        // 查询指定商品详情
        return Result.success(pmsProductService.getDetail(id));
    }
}
