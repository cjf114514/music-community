package com.music.interceptor;

import com.music.annotation.RequireAdmin;
import com.music.entity.User;
import com.music.repository.UserRepository;
import com.music.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员权限拦截器 — 校验用户是否有 ADMIN 角色
 *
 * ========== 与 JwtInterceptor 的区别 ==========
 *   JwtInterceptor:  校验"你是谁" (有没有登录)
 *   AdminInterceptor: 校验"你能干什么" (是不是管理员)
 *
 * ========== 工作方式 (基于注解) ==========
 *   1. 只拦截 /api/admin/** 下的请求 (由 WebConfig 配置)
 *   2. 通过反射检查请求对应的 Controller 方法上有没有 @RequireAdmin 注解
 *   3. 如果有 → 解析 Token → 查数据库获取用户 → 检查 role 是否为 "ADMIN"
 *   4. 如果不是管理员 → 返回 403
 *
 *   这种基于注解的权限控制优点:
 *     - 灵活: 同一个 Controller 里可以有公开方法和管理员方法
 *     - 明确: 看注解就知道哪些方法需要管理员权限
 *     - 无需每个方法里写重复的权限校验代码
 *
 * HandlerMethod 是什么?
 *   Spring MVC 中表示"将要执行的 Controller 方法"的对象
 *   通过它可以拿到方法的 Java 反射对象,进而读取方法上的注解
 */
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        // --- 第 1 步: 判断 handler 是否为 Controller 方法 ---
        // handler 可能是 Controller 方法,也可能是静态资源处理器
        // 只有 Controller 方法 (HandlerMethod) 才需要校验权限
        if (!(handler instanceof HandlerMethod)) {
            return true;  // 不是 Controller 方法,直接放行
        }

        // --- 第 2 步: 通过反射获取方法上的 @RequireAdmin 注解 ---
        HandlerMethod method = (HandlerMethod) handler;
        RequireAdmin annotation = method.getMethodAnnotation(RequireAdmin.class);

        // 如果方法上没有 @RequireAdmin 注解 → 不需要管理员权限,放行
        if (annotation == null) {
            return true;
        }

        // --- 第 3 步: 有 @RequireAdmin 注解 → 开始校验 ---
        // 先从请求中取 Token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            token = request.getParameter("token");
        }

        // 没有 Token → 拒绝 (不同于 JwtInterceptor,这里不分 GET/POST,一律要求登录)
        if (token == null) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"未登录\",\"data\":null}");
            return false;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // --- 第 4 步: 解析 Token → 查出用户 → 校验角色 ---
        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            User user = userRepository.findById(userId).orElse(null);

            if (user == null) {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"用户不存在\",\"data\":null}");
                return false;
            }

            // 核心校验: 角色必须是 "ADMIN"
            // User 实体中 role 字段默认值是 "USER",管理员账号是 "ADMIN"
            if (!"ADMIN".equals(user.getRole())) {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"权限不足，需要管理员权限\",\"data\":null}");
                return false;
            }

            // 权限通过 → 将 adminId 存入 request Attribute
            request.setAttribute("adminId", userId);
            return true;

        } catch (Exception e) {
            // Token 解析失败
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"Token无效\",\"data\":null}");
            return false;
        }
    }
}
