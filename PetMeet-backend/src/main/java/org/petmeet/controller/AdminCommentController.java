package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.service.CmsCommentService;
import org.petmeet.vo.CommentVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/comment")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "管理端评论", description = "管理端评论列表与删除")
public class AdminCommentController {

    private final CmsCommentService commentService;

    /**
     * 评论列表
     */
    @GetMapping("/list")
    @Operation(summary = "评论列表")
    public Result<Page<CommentVO>> list(
            @Parameter(description = "笔记ID") @RequestParam Long noteId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        // 查询指定笔记下的评论列表
        return Result.success(commentService.pageList(noteId, pageNum, pageSize));
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除评论")
    public Result<Void> delete(@PathVariable Long id) {
        // 调用业务层删除评论
        commentService.deleteComment(id);
        return Result.success("删除成功", null);
    }
}
