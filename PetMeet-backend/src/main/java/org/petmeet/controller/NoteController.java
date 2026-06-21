package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.dto.NotePublishDTO;
import org.petmeet.service.CmsNoteService;
import org.petmeet.vo.NoteDetailVO;
import org.petmeet.vo.NoteListVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
@Tag(name = "Note", description = "Note publish, list and detail")
public class NoteController {

    private final CmsNoteService cmsNoteService;

    /**
     * 发布笔记
     */
    @PostMapping("/publish")
    @SaCheckLogin
    @Operation(summary = "Publish note")
    public Result<Long> publish(@Valid @RequestBody NotePublishDTO dto) {
        // 调用业务层发布笔记
        Long noteId = cmsNoteService.publish(dto);
        return Result.success("已提交审核", noteId);
    }

    /**
     * 笔记详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "Note detail")
    public Result<NoteDetailVO> getDetail(@PathVariable Long id) {
        // 查询指定笔记详情
        return Result.success(cmsNoteService.getDetail(id));
    }

    /**
     * 笔记列表
     */
    @GetMapping("/list")
    @Operation(summary = "Note list")
    public Result<Page<NoteListVO>> list(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "Keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Product id") @RequestParam(required = false) Long productId,
            @Parameter(description = "Category") @RequestParam(required = false) String category,
            @Parameter(description = "Tags (comma separated)") @RequestParam(required = false) String tag) {
        // 查询前台笔记列表
        return Result.success(cmsNoteService.pageList(pageNum, pageSize, keyword, productId, category, tag));
    }

    /**
     * 我的笔记
     */
    @GetMapping("/my")
    @SaCheckLogin
    @Operation(summary = "My notes")
    public Result<Page<NoteListVO>> myNotes(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 查询当前用户发布的笔记
        return Result.success(cmsNoteService.pageMyNotes(pageNum, pageSize));
    }

    /**
     * 我的收藏
     */
    @GetMapping("/my/collect")
    @SaCheckLogin
    @Operation(summary = "My collected notes")
    public Result<Page<NoteListVO>> myCollectedNotes(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 查询当前用户收藏的笔记
        return Result.success(cmsNoteService.pageMyCollectedNotes(pageNum, pageSize));
    }

    /**
     * 我的点赞
     */
    @GetMapping("/my/like")
    @SaCheckLogin
    @Operation(summary = "My liked notes")
    public Result<Page<NoteListVO>> myLikedNotes(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 查询当前用户点赞的笔记
        return Result.success(cmsNoteService.pageMyLikedNotes(pageNum, pageSize));
    }

    /**
     * 点赞笔记
     */
    @PostMapping("/like/{id}")
    @SaCheckLogin
    @Operation(summary = "Like or unlike")
    public Result<Boolean> like(@PathVariable Long id) {
        // 切换当前用户的点赞状态
        boolean liked = cmsNoteService.toggleLike(id);
        return Result.success(liked ? "已点赞" : "已取消点赞", liked);
    }

    /**
     * 收藏笔记
     */
    @PostMapping("/collect/{id}")
    @SaCheckLogin
    @Operation(summary = "Collect or uncollect")
    public Result<Boolean> collect(@PathVariable Long id) {
        // 切换当前用户的收藏状态
        boolean collected = cmsNoteService.toggleCollect(id);
        return Result.success(collected ? "已收藏" : "已取消收藏", collected);
    }

    /**
     * 推荐笔记
     */
    @PostMapping("/recommend/{id}")
    @SaCheckLogin
    @Operation(summary = "Recommend or cancel")
    public Result<Boolean> recommend(@PathVariable Long id) {
        // 切换当前用户的推荐状态
        boolean recommended = cmsNoteService.toggleRecommend(id);
        return Result.success(recommended ? "已推荐" : "已取消推荐", recommended);
    }

    /**
     * 下架笔记
     */
    @PutMapping("/my/{id}/shelf")
    @SaCheckLogin
    @Operation(summary = "My note off-shelf / restore")
    public Result<Boolean> toggleMyShelf(@PathVariable Long id) {
        // 切换当前用户笔记的上下架状态
        boolean offShelf = cmsNoteService.toggleMyShelf(id);
        return Result.success(offShelf ? "已下架" : "已恢复上架", offShelf);
    }

    /**
     * 下架笔记
     */
    @PostMapping("/my/{id}/shelf")
    @SaCheckLogin
    @Operation(summary = "My note off-shelf / restore (POST fallback)")
    public Result<Boolean> toggleMyShelfPost(@PathVariable Long id) {
        // 兼容 POST 方式切换笔记上下架状态
        boolean offShelf = cmsNoteService.toggleMyShelf(id);
        return Result.success(offShelf ? "已下架" : "已恢复上架", offShelf);
    }

    /**
     * 删除笔记
     */
    @DeleteMapping("/my/{id}")
    @SaCheckLogin
    @Operation(summary = "Delete my note (soft status delete)")
    public Result<Void> deleteMyNote(@PathVariable Long id) {
        // 调用业务层删除当前用户笔记
        cmsNoteService.deleteMyNote(id);
        return Result.success("已删除", null);
    }

    /**
     * 删除笔记
     */
    @PostMapping("/my/{id}/delete")
    @SaCheckLogin
    @Operation(summary = "Delete my note (POST fallback)")
    public Result<Void> deleteMyNotePost(@PathVariable Long id) {
        // 兼容 POST 方式删除当前用户笔记
        cmsNoteService.deleteMyNote(id);
        return Result.success("已删除", null);
    }
}
