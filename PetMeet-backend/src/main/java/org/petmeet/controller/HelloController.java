package org.petmeet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.petmeet.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zjx
 */
@RestController
@RequestMapping("/hello")
@Tag(name = "测试接口", description = "用于测试服务是否正常运行")
public class HelloController {

    /**
     * Hello 测试
     */
    @GetMapping
    @Operation(summary = "Hello 测试")
    public Result<String> hello() {
        return Result.success("欢迎使用宠遇 PetMeet API！");
    }

    /**
     * 问候接口
     */
    @GetMapping("/greet")
    @Operation(summary = "问候接口")
    public Result<String> greet(@RequestParam(defaultValue = "朋友") String name) {
        return Result.success("你好，" + name + "！欢迎来到宠遇！");
    }

    /**
     * 服务状态
     */
    @GetMapping("/status")
    @Operation(summary = "服务器状态")
    public Result<Map<String, Object>> status() {
        // 组装服务运行状态信息
        Map<String, Object> status = new HashMap<>();
        status.put("status", "running");
        status.put("time", LocalDateTime.now().toString());
        status.put("version", "v1.0.0");
        status.put("app", "PetMeet - 宠遇内容电商平台");
        return Result.success(status);
    }
}
