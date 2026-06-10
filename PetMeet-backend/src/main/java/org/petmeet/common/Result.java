package org.petmeet.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "API统一响应结果")
public class Result<T> {
    @Schema(description = "状态码：200成功，401未登录，403无权限，500失败")
    private Integer code;

    @Schema(description = "提示信息")
    private String msg;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "是否成功")
    private boolean success;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        result.setSuccess(true);
        return result;
    }

    public static Result<Void> success() {
        Result<Void> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(null);
        result.setSuccess(true);
        return result;
    }

    public static <T> Result<T> success(String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg(msg);
        result.setData(data);
        result.setSuccess(true);
        return result;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMsg(msg);
        result.setData(null);
        result.setSuccess(false);
        return result;
    }

    public static <T> Result<T> notLogin(String msg) {
        Result<T> result = new Result<>();
        result.setCode(401);
        result.setMsg(msg);
        result.setData(null);
        result.setSuccess(false);
        return result;
    }

    public static <T> Result<T> noPermission(String msg) {
        Result<T> result = new Result<>();
        result.setCode(403);
        result.setMsg(msg);
        result.setData(null);
        result.setSuccess(false);
        return result;
    }
}
