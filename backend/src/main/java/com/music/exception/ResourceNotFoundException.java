package com.music.exception;

/**
 * 资源不存在异常
 *
 * 用来区分两类完全不同的失败：
 *
 *   ① 「你请求的东西不存在」—— 歌单被删了、歌曲下架了、帖子隐藏了
 *      这是**正常的业务结果**，应该返回 HTTP 404
 *
 *   ② 「服务器出问题了」—— 数据库连不上、空指针、逻辑 bug
 *      这才是 HTTP 500
 *
 * 为什么要区分？
 *   · 前端能据此给出不同反馈：404 → 显示「歌单不存在或已被删除」+ 返回按钮；
 *     500 → 显示「服务器开小差了，请稍后重试」
 *   · 监控系统不会因为用户点了失效链接就报「服务异常」
 *   · 日志里 500 变少，真正的故障更容易被发现
 *
 * 用法：Controller / Service 里发现资源为空时直接抛
 *   throw new ResourceNotFoundException("歌单不存在");
 * 由 GlobalExceptionHandler 统一转成 404 响应。
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
