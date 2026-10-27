package com.distributed.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * <p>统一捕获运行时异常，返回标准 JSON 错误响应，
 * 避免 Spring 默认错误页泄露堆栈信息。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理所有未捕获的运行时异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("[GlobalExceptionHandler] 未处理异常: {}", e.getMessage(), e);

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", e.getClass().getSimpleName());
        body.put("message", e.getMessage());
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e) {
        log.warn("[GlobalExceptionHandler] 业务异常: {}", e.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("code", e.getCode());
        body.put("error", "BusinessException");
        body.put("message", e.getMessage());
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(e.getCode() >= 400 && e.getCode() < 500 ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * 处理参数校验失败异常
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("[GlobalExceptionHandler] 参数校验失败: {}", msg);

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("code", 400);
        body.put("error", "ValidationException");
        body.put("message", msg);
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[GlobalExceptionHandler] 非法参数: {}", e.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("code", 400);
        body.put("error", "IllegalArgument");
        body.put("message", e.getMessage());
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
