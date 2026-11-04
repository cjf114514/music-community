package com.music.exception;

import com.music.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * ========== 为什么需要它？==========
 * Controller / Service 里抛出的异常如果没有被捕获，
 * 没有这个处理器 → Spring 返回默认的 500 错误页（格式不可控）
 * 有了这个处理器 → 自动捕获 → 返回格式统一的 Result + 合适的 HTTP 状态码
 *
 * ========== 错误分层 ==========
 *   404 ResourceNotFoundException —— 资源不存在（歌单被删、歌曲下架…）
 *   500 RuntimeException        —— 真正的服务端问题（"失败"）
 *   401 由 JwtInterceptor 直接返回，不经过这里
 *
 * 以前的实现把所有异常都当 500 返回，而且 HTTP 状态码永远是 200，
 * 导致"用户点了失效链接"和"服务器崩了"在前端看起来一模一样。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 资源不存在 → HTTP 404 + code 404
     *
     * @ResponseStatus 让响应真正带上 404 状态码，
     * 前端 axios 可以直接用 error.response.status 判断
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(ResourceNotFoundException e) {
        log.debug("资源不存在: {}", e.getMessage());
        return Result.error(404, e.getMessage());
    }

    /**
     * 其他运行时异常 → HTTP 500 + code 500
     *
     * 这里兼作兜底：项目里还有大量历史代码用
     *   throw new RuntimeException("歌单不存在")
     * 来表达"不存在"。如果消息以「不存在」结尾，就按 404 处理，
     * 避免为了改语义去动二十多处代码。
     *
     * 注意：返回 ResponseEntity 而不是加 @ResponseStatus，
     * 因为同一个处理器要输出两种状态码（404 / 500），
     * 写死 @ResponseStatus 会出现「HTTP 500 但 body.code=404」的矛盾。
     *
     * 后续新增代码请直接抛 ResourceNotFoundException，不要依赖这个消息匹配。
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Void>> handleRuntimeException(RuntimeException e) {
        String msg = e.getMessage();

        if (msg != null && msg.endsWith("不存在")) {
            log.debug("资源不存在(兼容旧消息): {}", msg);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error(404, msg));
        }

        log.warn("服务端异常: {}", msg, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(msg == null ? "服务器内部错误" : msg));
    }
}
