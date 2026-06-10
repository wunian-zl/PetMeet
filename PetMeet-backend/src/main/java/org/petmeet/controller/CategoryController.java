package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.entity.PmsCategory;
import org.petmeet.service.PmsCategoryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
@SaCheckRole("admin")
@Tag(name = "商品分类管理接口", description = "后台分类增删改查")
public class CategoryController {

    private final PmsCategoryService categoryService;

    /**
     * 分类列表
     */
    @GetMapping("/list/all")
    @Operation(summary = "获取所有分类(后台)")
    public Result<List<PmsCategory>> listAll() {
        // 查询全部分类
        return Result.success(categoryService.list());
    }

    /**
     * 新增分类
     */
    @PostMapping("/add")
    @Operation(summary = "新增分类")
    public Result<Boolean> add(@RequestBody PmsCategory category) {
        // 保存分类信息
        return Result.success(categoryService.save(category));
    }

    /**
     * 修改分类
     */
    @PostMapping("/update")
    @Operation(summary = "修改分类")
    public Result<Boolean> update(@RequestBody PmsCategory category) {
        // 根据 id 更新分类信息
        return Result.success(categoryService.updateById(category));
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除分类")
    public Result<Boolean> delete(@PathVariable Long id) {
        // 根据 id 删除分类
        return Result.success(categoryService.removeById(id));
    }
}
