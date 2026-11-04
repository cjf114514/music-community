package com.music.interceptor;

import com.music.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT 认证拦截器 — 每个请求的"守门人"
 *
 * ========== 拦截器是什么？==========
 * Spring MVC 中的一种 AOP 机制
 * 所有请求在到达 Controller 之前,都会先经过拦截器的 preHandle() 方法
 * preHandle 返回 true  → 放行,继续往下走
 * preHandle 返回 false → 拒绝,直接返回响应(不会进入 Controller)
 *
 * ========== 本拦截器的职责 ==========
 *   1. 从请求头 Authorization 中提取 JWT Token
 *   2. 验证 Token 是否有效
 *   3. 解析出 userId,放入 request Attribute (传给 Controller 使用)
 *
 * ========== 放行策略 ==========
 *   1. 完全没带 Token:
 *        GET  → 放行 (游客浏览公开内容)
 *        其他 → 401 未登录
 *   2. 带了 Token 但无效/过期:
 *        一律 401 (不管 GET 还是 POST)
 *        这样前端能立刻清理登录态并跳转登录页,而不是让请求带着 userId=null
 *        一路走进 Service 抛 500
 *
 *   遗留问题(待改进): 隐私类 GET 接口(私信/通知)在"完全没带 Token"时
 *   仍会被放行,需要在 Controller 层额外校验 userId,或收窄放行白名单。
 *
 * @Component  → 标记为 Spring Bean,让 WebConfig 可以注入它
 * @RequiredArgsConstructor → Lombok 为所有 final 字段生成构造方法
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    /**
     * 拦截器的核心方法,每次请求都会调用
     *
     * @param request   HTTP 请求 (可以从中取 Header、参数等)
     * @param response  HTTP 响应 (可以直接写入响应内容)
     * @param handler   处理器对象 (即将处理请求的 Controller 方法)
     * @return true = 放行, false = 拒绝
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        // --- 第 0 步: 放行所有 OPTIONS 预检请求 ---
        // 浏览器在跨域 POST/PUT 之前会先发一个 OPTIONS 请求(预检)
        // 这个请求不带 Token,必须直接放行,否则跨域请求全部失败
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // --- 第 1 步: 尝试从不同位置获取 Token ---
        // 优先从 Authorization 请求头获取
        // Authorization 头格式: "Bearer eyJhbGciOiJIUzI1NiJ9..."
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            // 如果请求头没有,尝试从 URL 参数获取 (如:下载链接 /api/songs/1/download?token=xxx)
            token = request.getParameter("token");
        }

        // --- 第 2 步: 没有 Token 的情况 ---
        if (token == null) {
            // GET 请求 → 放行 (游客可以浏览公开内容)
            // 原因: 歌曲列表、歌单详情、评论等公开内容不需要登录
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                return true;
            }
            // 非 GET 请求 (POST/PUT/DELETE) → 拒绝
            return unauthorized(response, "未登录，请先登录");
        }

        // --- 第 3 步: 去掉 "Bearer " 前缀,得到纯 Token ---
        if (token.startsWith("Bearer ")) {
            token = token.substring(7); // "Bearer " 共 7 个字符
        }

        // --- 第 4 步: 验证 Token ---
        //
        // 注意: 这里不再区分 GET / POST —— 只要请求带了 Token,它就必须有效。
        //
        // 为什么? 旧逻辑对「带了过期 Token 的 GET」也放行,导致:
        //   Token 过期后, 我的收藏/我的歌单/私信等接口会带着 userId=null 进入 Controller,
        //   在 Service 层抛 "The given id must not be null", 返回 code=500。
        //   前端只认 401, 所以既不清理 Token 也不跳登录页,
        //   用户看到的是"服务器错误", 必须自己猜出"该重新登录了"。
        // 现在改成直接返回 401, 前端拦截器会清理登录态并跳转登录页。
        try {
            if (jwtUtil.isTokenExpired(token)) {
                return unauthorized(response, "登录已过期，请重新登录");
            }

            // Token 有效 → 解析出 userId
            // 存入 request Attribute,Controller 通过 request.getAttribute("userId") 获取
            Long userId = jwtUtil.getUserIdFromToken(token);
            request.setAttribute("userId", userId);
            return true;  // 放行

        } catch (Exception e) {
            // 解析失败 (签名不对/格式错误等)
            return unauthorized(response, "登录状态无效，请重新登录");
        }
    }

    /**
     * 统一的未认证响应
     * HTTP 状态码和业务 code 都设为 401，前端 axios 拦截器据此清理登录态并跳转登录页
     */
    private boolean unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
        return false;
    }
}
