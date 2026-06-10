package org.petmeet.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.common.Result;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(basePackages = "org.petmeet.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.warn("Not login: {}", e.getMessage());
        String message;
        switch (e.getType()) {
            case NotLoginException.NOT_TOKEN:
                message = "Token not provided";
                break;
            case NotLoginException.INVALID_TOKEN:
                message = "Invalid token";
                break;
            case NotLoginException.TOKEN_TIMEOUT:
                message = "Token expired";
                break;
            case NotLoginException.BE_REPLACED:
                message = "Account logged in elsewhere";
                break;
            case NotLoginException.KICK_OUT:
                message = "Account kicked out";
                break;
            default:
                message = "Please login first";
        }
        return Result.notLogin(message);
    }

    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e) {
        log.warn("No permission: {}", e.getPermission());
        return Result.noPermission("No permission: " + e.getPermission());
    }

    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e) {
        log.warn("No role: {}", e.getRole());
        return Result.noPermission("Role required: " + e.getRole());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        return Result.error(message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Bind failed: {}", message);
        return Result.error(message);
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("Runtime exception: ", e);
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("System exception: ", e);
        return Result.error("System busy, please try later");
    }
}
