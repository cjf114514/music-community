package com.music.controller;

import com.music.common.Result;
import com.music.exception.UnauthorizedException;
import com.music.service.ProgressService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 播放进度 —— 断点续播 / 跨设备续听
 *
 * PUT    /api/progress            上报位置 { songId, position }（position <= 5 视为从头/结束）
 * GET    /api/progress/{songId}   查询某首歌上次听到哪里
 * GET    /api/progress/resume     最近一次未播完的歌（用于打开网站时"继续上次播放"）
 * DELETE /api/progress/{songId}   清除某首的进度
 *
 * 全部按当前登录用户隔离，JwtInterceptor 会拦截未登录请求。
 */
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PutMapping
    public Result<Void> save(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        Long userId = requireUser(request);
        Long songId = body.get("songId");
        Long position = body.get("position");
        if (songId == null || position == null) {
            return Result.error("参数不完整");
        }
        progressService.savePosition(userId, songId, position.intValue());
        return Result.success();
    }

    @GetMapping("/{songId}")
    public Result<Map<String, Object>> get(@PathVariable Long songId, HttpServletRequest request) {
        Long userId = requireUser(request);
        int position = progressService.getPosition(userId, songId);
        return Result.success(Map.of("songId", songId, "position", position));
    }

    @GetMapping("/resume")
    public Result<Map<String, Object>> resume(HttpServletRequest request) {
        Long userId = requireUser(request);
        return Result.success(progressService.resume(userId));
    }

    @DeleteMapping("/{songId}")
    public Result<Void> clear(@PathVariable Long songId, HttpServletRequest request) {
        Long userId = requireUser(request);
        progressService.clear(userId, songId);
        return Result.success();
    }

    /**
     * 取当前登录用户
     *
     * JwtInterceptor 对「没带 Token 的 GET」仍然放行（历史兼容），
     * 所以必须登录的接口要自己校验一次，否则会带着 userId=null 走进业务层。
     */
    private Long requireUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("请先登录后再使用续播功能");
        }
        return userId;
    }
}
