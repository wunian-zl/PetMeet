package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.dto.CommentCreateDTO;
import org.petmeet.service.CmsCommentService;
import org.petmeet.vo.CommentVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
@Tag(name = "Comment", description = "Comment APIs")
public class CommentController {

    private final CmsCommentService commentService;

    @GetMapping("/list")
    @Operation(summary = "Comment list")
    public Result<Page<CommentVO>> list(
            @Parameter(description = "Note id") @RequestParam Long noteId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(commentService.pageList(noteId, pageNum, pageSize));
    }

    @GetMapping("/{id}/replies")
    @Operation(summary = "Comment replies")
    public Result<Page<CommentVO>> replies(
            @PathVariable Long id,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(commentService.pageReplies(id, pageNum, pageSize));
    }

    @PostMapping("/add")
    @SaCheckLogin
    @Operation(summary = "Create comment")
    public Result<Long> add(@Valid @RequestBody CommentCreateDTO dto) {
        Long id = commentService.addComment(dto);
        return Result.success("评论已发布", id);
    }

    @PostMapping("/{id}/like")
    @SaCheckLogin
    @Operation(summary = "Toggle comment like")
    public Result<Boolean> like(@PathVariable Long id) {
        return Result.success(commentService.toggleLike(id));
    }

    @DeleteMapping("/{id}")
    @SaCheckLogin
    @Operation(summary = "Delete comment")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success("评论已删除", null);
    }
}
