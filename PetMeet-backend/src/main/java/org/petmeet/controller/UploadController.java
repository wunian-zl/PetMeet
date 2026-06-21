package org.petmeet.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.common.Result;
import org.petmeet.support.UploadPathResolver;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 通用文件上传控制器
 *
 * @author zjx
 */
@Slf4j
@RestController
@RequestMapping("/common")
@Tag(name = "通用接口", description = "文件上传等通用功能")
public class UploadController {

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp");

    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp",
            "mp4", "avi", "mov",
            "pdf", "doc", "docx", "xls", "xlsx");

    private static final Set<String> VIDEO_TYPES = Set.of("mp4", "avi", "mov");
    private static final Set<String> IMAGE_BIZ_TYPES = Set.of(
            "userAvatar", "userImage", "productCover", "productDetail",
            "shopBannerImage", "noteImage", "scienceTopicImage", "complaintEvidence");
    private static final Set<String> VIDEO_BIZ_TYPES = Set.of("userVideo", "noteVideo");
    private static final Set<String> ADMIN_ONLY_BIZ_TYPES = Set.of(
            "productCover", "productDetail", "shopBannerImage", "scienceTopicImage");

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final Map<String, String> BIZ_DIR_MAP = Map.ofEntries(
            Map.entry("common", "common"),
            // 用户
            Map.entry("userAvatar", "user/avatar"),
            Map.entry("userImage", "user/media/image"),
            Map.entry("userVideo", "user/media/video"),
            // 商品
            Map.entry("productCover", "product/cover"),
            Map.entry("productDetail", "product/detail"),
            // 商城广告位（cms_banner）
            Map.entry("shopBannerImage", "shop/banner/image"),
            // 社区笔记
            Map.entry("noteImage", "note/image"),
            Map.entry("noteVideo", "note/video"),
            // 投诉凭证
            Map.entry("complaintEvidence", "complaint/evidence"),
            // 科普栏目（cms_banner）
            Map.entry("scienceTopicImage", "science/topic/image")
    );

    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM");

    /**
     * 单文件上传
     */
    @PostMapping("/upload")
    @Operation(summary = "单文件上传", description = "上传单个文件，返回虚拟访问路径")
    public Result<String> upload(
            @Parameter(description = "上传的文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务标识（决定保存目录）") @RequestParam(required = false) String biz) {
        checkUploadPermission(biz);
        return saveFile(file, biz);
    }

    private Result<String> saveFile(MultipartFile file, String biz) {
        // 校验文件是否为空
        if (file == null || file.isEmpty()) {
            return Result.badRequest("请选择要上传的文件");
        }

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.badRequest("文件大小不能超过10MB");
        }

        // 读取原文件名和后缀
        String originalFilename = file.getOriginalFilename();
        String extension = FileUtil.extName(originalFilename);

        if (StrUtil.isBlank(extension)) {
            return Result.badRequest("无法识别文件类型");
        }

        // 校验文件类型是否合法
        if (!ALLOWED_FILE_TYPES.contains(extension.toLowerCase())) {
            return Result.badRequest("不支持的文件类型:" + extension);
        }

        String normalizedExtension = extension.toLowerCase();
        String bizKey = normalizeBiz(biz);
        if (IMAGE_BIZ_TYPES.contains(bizKey) && !ALLOWED_IMAGE_TYPES.contains(normalizedExtension)) {
            return Result.badRequest("当前业务仅支持图片文件");
        }
        if (VIDEO_BIZ_TYPES.contains(bizKey) && !VIDEO_TYPES.contains(normalizedExtension)) {
            return Result.badRequest("当前业务仅支持视频文件");
        }
        if (!matchesFileSignature(file, normalizedExtension)) {
            return Result.badRequest("文件内容与扩展名不匹配");
        }

        try {
            // 生成新的文件名
            String newFileName = IdUtil.simpleUUID() + "." + normalizedExtension;

            // 根据业务标识确定保存目录
            String bizDir = resolveBizDir(biz);
            if (bizDir == null) {
                return Result.badRequest("不支持的业务类型biz:" + biz);
            }

            // 生成按年月分类的目录结构
            LocalDate today = LocalDate.now();
            String yyyy = today.format(YEAR_FMT);
            String mm = today.format(MONTH_FMT);

            // 项目根目录/uploads/<bizDir>/<yyyy>/<mm>/
            File uploadDir = getUploadRootDir();
            File targetDir = new File(uploadDir, bizDir + File.separator + yyyy + File.separator + mm);

            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            // 把文件保存到磁盘
            File destFile = new File(targetDir, newFileName);
            file.transferTo(destFile);

            // 虚拟访问路径（前端通过 resolveImageUrl / getImageUrl 使用）
            // /images/<bizDir>/<yyyy>/<mm>/<filename>
            String virtualPath = "/images/" + bizDir.replace("\\", "/") + "/" + yyyy + "/" + mm + "/" + newFileName;
            log.info("文件上传成功: {} -> {}", originalFilename, virtualPath);

            return Result.success("上传成功", virtualPath);

        } catch (IOException e) {
            log.error("文件上传失败: ", e);
            return Result.error("文件上传失败，请稍后重试");
        }
    }

    /**
     * 图片上传
     */
    @PostMapping("/upload/image")
    @Operation(summary = "上传图片", description = "仅支持上传图片类型文件")
    public Result<String> uploadImage(
            @Parameter(description = "上传的图片", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务标识（决定保存目录）") @RequestParam(required = false) String biz) {
        checkUploadPermission(biz);

        // 校验图片是否为空
        if (file == null || file.isEmpty()) {
            return Result.badRequest("请选择要上传的图片");
        }

        // 校验图片格式
        String extension = FileUtil.extName(file.getOriginalFilename());
        if (StrUtil.isBlank(extension) || !ALLOWED_IMAGE_TYPES.contains(extension.toLowerCase())) {
            return Result.badRequest("仅支持上传图片格式:" + String.join(",", ALLOWED_IMAGE_TYPES));
        }

        // 复用单文件上传逻辑
        return saveFile(file, biz);
    }

    /**
     * 批量文件上传
     */
    @PostMapping("/upload/batch")
    @Operation(summary = "批量文件上传", description = "上传多个文件，返回访问URL列表")
    public Result<List<String>> uploadBatch(
            @Parameter(description = "上传的文件列表", required = true) @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "业务标识（决定保存目录）") @RequestParam(required = false) String biz) {
        checkUploadPermission(biz);

        // 校验文件数组是否为空
        if (files == null || files.length == 0) {
            return Result.badRequest("请选择要上传的文件");
        }

        // 限制单次上传数量
        if (files.length > 9) {
            return Result.badRequest("一次最多上传9个文件");
        }

        List<String> urls = new java.util.ArrayList<>();

        // 循环调用单文件上传逻辑
        for (MultipartFile file : files) {
            Result<String> result = saveFile(file, biz);
            if (result.isSuccess()) {
                urls.add(result.getData());
            } else {
                return Result.badRequest("文件" + file.getOriginalFilename() + "上传失败:" + result.getMsg());
            }
        }

        return Result.success("批量上传成功", urls);
    }

    private File getUploadRootDir() {
        // 解析 uploads 根目录
        File uploadDir = UploadPathResolver.resolveUploadsRootDir();
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        return uploadDir;
    }

    private void checkUploadPermission(String biz) {
        StpUtil.checkLogin();
        if (ADMIN_ONLY_BIZ_TYPES.contains(normalizeBiz(biz))) {
            StpUtil.checkRole("admin");
        }
    }

    private String normalizeBiz(String biz) {
        return StrUtil.isBlank(biz) ? "common" : biz.trim();
    }

    private String resolveBizDir(String biz) {
        // 空业务标识默认落到 common 目录
        return BIZ_DIR_MAP.get(normalizeBiz(biz));
    }

    private boolean matchesFileSignature(MultipartFile file, String extension) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(16);
            return switch (extension) {
                case "jpg", "jpeg" -> startsWith(header, 0xff, 0xd8, 0xff);
                case "png" -> startsWith(header, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a);
                case "gif" -> startsWithAscii(header, "GIF87a") || startsWithAscii(header, "GIF89a");
                case "webp" -> startsWithAscii(header, "RIFF")
                        && hasAsciiAt(header, 8, "WEBP");
                case "bmp" -> startsWithAscii(header, "BM");
                case "pdf" -> startsWithAscii(header, "%PDF-");
                case "doc", "xls" -> startsWith(header, 0xd0, 0xcf, 0x11, 0xe0, 0xa1, 0xb1, 0x1a, 0xe1);
                case "docx" -> matchesOfficeZip(file, "word/");
                case "xlsx" -> matchesOfficeZip(file, "xl/");
                case "mp4", "mov" -> hasAsciiAt(header, 4, "ftyp");
                case "avi" -> startsWithAscii(header, "RIFF")
                        && hasAsciiAt(header, 8, "AVI ");
                default -> false;
            };
        } catch (IOException e) {
            log.warn("读取上传文件签名失败: {}", file.getOriginalFilename(), e);
            return false;
        }
    }

    private boolean matchesOfficeZip(MultipartFile file, String requiredPrefix) {
        boolean hasContentTypes = false;
        boolean hasRequiredDirectory = false;
        int inspectedEntries = 0;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null && inspectedEntries++ < 200) {
                String name = entry.getName();
                if ("[Content_Types].xml".equals(name)) {
                    hasContentTypes = true;
                }
                if (name.startsWith(requiredPrefix)) {
                    hasRequiredDirectory = true;
                }
                if (hasContentTypes && hasRequiredDirectory) {
                    return true;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    private boolean startsWith(byte[] bytes, int... expected) {
        if (bytes.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((bytes[i] & 0xff) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWithAscii(byte[] bytes, String expected) {
        return hasAsciiAt(bytes, 0, expected);
    }

    private boolean hasAsciiAt(byte[] bytes, int offset, String expected) {
        if (bytes.length < offset + expected.length()) {
            return false;
        }
        for (int i = 0; i < expected.length(); i++) {
            if (bytes[offset + i] != (byte) expected.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
