package com.music.exception;

/**
 * 未认证异常 → 由 GlobalExceptionHandler 转成 HTTP 401
 *
 * 为什么单独需要它？
 *   JwtInterceptor 出于兼容考虑，对「完全没带 Token 的 GET 请求」仍然放行
 *   （历史代码里有一批公开接口依赖这个行为）。这导致某些必须登录才能用的
 *   GET 接口在未登录时也能进来，只是 userId 为 null。
 *
 *   需要在 Controller 里自己校验的地方，就抛这个异常，
 *   统一得到 401，前端拦截器会自动清理登录态并跳转登录页。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
