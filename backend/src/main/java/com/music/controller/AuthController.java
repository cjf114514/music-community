package com.music.controller;

import com.music.common.Result;
import com.music.dto.LoginRequest;
import com.music.dto.LoginResponse;
import com.music.dto.RegisterRequest;
import com.music.entity.User;
import com.music.repository.CommunityPostRepository;
import com.music.repository.PlayHistoryRepository;
import com.music.repository.PlaylistRepository;
import com.music.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器 — 处理登录、注册、用户信息管理
 *
 * Controller 层职责:
 *   只负责"接请求 → 调 Service → 返回结果",不写业务逻辑
 *   业务逻辑全部在 Service 层,Controller 只做参数提取和结果包装
 *
 * @RestController = @Controller + @ResponseBody
 *   所有方法的返回值自动序列化为 JSON
 *
 * @RequestMapping("/api/auth")
 *   下面所有接口的完整路径都以 /api/auth 开头
 *   例如 @PostMapping("/login") → 完整路径 = /api/auth/login
 *
 * 如何拿到当前登录用户？
 *   request.getAttribute("userId") — JwtInterceptor 解析 Token 后存入的
 *   前端不需要传 userId,后端从 Token 解析 → 防止用户伪造身份
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    // 下面三个 Repository 直接注入了 Controller (通常不推荐,应该通过 Service 封装)
    // 这里是为了统计方便做的简化
    private final PlaylistRepository playlistRepository;
    private final CommunityPostRepository communityPostRepository;
    private final PlayHistoryRepository playHistoryRepository;

    /**
     * 登录 — POST /api/auth/login
     *
     * @Valid: 自动校验 LoginRequest 中的字段约束 (@NotBlank, @Email 等)
     *   校验失败会返回 400,不会进入方法体
     * @RequestBody: JSON → Java 对象自动转换
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /** 注册 — POST /api/auth/register */
    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    /** 获取当前用户信息 — GET /api/auth/info */
    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(authService.getUserInfo(userId));
    }

    /** 更新个人信息 — PUT /api/auth/info (只更新前端传了非 null 的字段) */
    @PutMapping("/info")
    public Result<User> updateUserInfo(HttpServletRequest request, @RequestBody User userInfo) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(authService.updateUserInfo(userId, userInfo));
    }

    /** 修改密码 (需要旧密码) — PUT /api/auth/change-password */
    @PutMapping("/change-password")
    public Result<Void> changePassword(HttpServletRequest request, @RequestBody Map<String, String> params) {
        Long userId = (Long) request.getAttribute("userId");
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        authService.changePassword(userId, oldPassword, newPassword);
        return Result.success();
    }

    /** 重置密码 (通过邮箱验证码,忘记密码时用) — POST /api/auth/reset-password */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        String code = params.get("code");
        String newPassword = params.get("newPassword");
        authService.resetPassword(email, code, newPassword);
        return Result.success();
    }

    /** 更换绑定邮箱 — PUT /api/auth/change-email */
    @PutMapping("/change-email")
    public Result<Void> changeEmail(HttpServletRequest request, @RequestBody Map<String, String> params) {
        Long userId = (Long) request.getAttribute("userId");
        String newEmail = params.get("newEmail");
        String code = params.get("code");
        authService.changeEmail(userId, newEmail, code);
        return Result.success();
    }

    /**
     * 用户统计 — GET /api/auth/user-stats/123
     *
     * @PathVariable: 把 URL 中的 {userId} 绑定到方法参数
     *
     * 注意: playCount 查询效率低(查全表再内存过滤),数据量大时需改进
     */
    @GetMapping("/user-stats/{userId}")
    public Result<Map<String, Object>> getUserStats(@PathVariable Long userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("playlistCount", playlistRepository.findByUserIdAndStatus(userId, 1).size());
        stats.put("postCount", communityPostRepository.findByUserIdOrderByCreateTimeDesc(userId,
                org.springframework.data.domain.PageRequest.of(0, 1)).size());
        long playCount = playHistoryRepository.findAllByOrderByPlayTimeDesc().stream()
                .filter(ph -> ph.getUserId().equals(userId)).count();
        stats.put("playCount", playCount);
        return Result.success(stats);
    }
}
