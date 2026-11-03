package com.luzhiwen.collectagent.shared.error;

import com.luzhiwen.collectagent.conversation.application.SessionClosedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理器，统一返回 API 错误结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 当前类日志器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理请求参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("请求参数不合法");
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 处理 HTTP 方法不支持异常。
     *
     * @param exception HTTP 方法不支持异常
     * @return 统一错误响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception
    ) {
        return buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "请求方法不支持，请使用 POST 提交 JSON 请求体"
        );
    }

    /**
     * 处理接口或静态资源不存在异常。
     *
     * @param exception 资源不存在异常
     * @return 统一错误响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFoundException(
            NoResourceFoundException exception
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "接口或静态资源不存在");
    }
    /**
     * 处理业务参数异常。
     *
     * @param exception 参数异常
     * @return 统一错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException exception
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /**
     * 处理已关闭会话继续聊天的业务异常。
     *
     * @param exception 已关闭会话异常
     * @return 统一错误响应
     */
    @ExceptionHandler(SessionClosedException.class)
    public ResponseEntity<Map<String, Object>> handleSessionClosedException(
            SessionClosedException exception
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    /**
     * 处理未预期异常并记录完整堆栈。
     *
     * @param exception 未预期异常
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception exception) {
        LOGGER.error("Agent 请求处理失败", exception);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "系统处理失败，请稍后重试");
    }

    /**
     * 构建统一错误响应。
     *
     * @param status HTTP 状态
     * @param message 错误信息
     * @return 错误响应实体
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}



