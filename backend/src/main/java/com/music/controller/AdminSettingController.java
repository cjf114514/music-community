package com.music.controller;

import com.music.annotation.OperationLogAnnotation;
import com.music.annotation.RequireAdmin;
import com.music.common.Result;
import com.music.entity.PlayHistory;
import com.music.entity.SearchHistory;
import com.music.repository.PlayHistoryRepository;
import com.music.repository.SearchHistoryRepository;
import com.music.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingController {

    private final SystemSettingService systemSettingService;
    private final PlayHistoryRepository playHistoryRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    @GetMapping
    @RequireAdmin
    public Result<Map<String, Object>> getSettings() {
        Map<String, Object> result = new HashMap<>();
        result.put("allowRegister", systemSettingService.getBoolean("allow_register", true));
        result.put("commentReview", systemSettingService.getBoolean("comment_review", false));
        result.put("communityEnabled", systemSettingService.getBoolean("community_enabled", true));
        return Result.success(result);
    }

    @PutMapping
    @RequireAdmin
    @OperationLogAnnotation(module = "系统设置", action = "UPDATE", description = "修改系统设置", targetType = "setting")
    public Result<Void> updateSettings(@RequestBody Map<String, Object> params) {
        if (params.containsKey("allowRegister")) {
            systemSettingService.set("allow_register", String.valueOf(params.get("allowRegister")), "允许新用户注册");
        }
        if (params.containsKey("commentReview")) {
            systemSettingService.set("comment_review", String.valueOf(params.get("commentReview")), "新评论需要审核");
        }
        if (params.containsKey("communityEnabled")) {
            systemSettingService.set("community_enabled", String.valueOf(params.get("communityEnabled")), "社区功能开关");
        }
        return Result.success();
    }

    @DeleteMapping("/play-history")
    @RequireAdmin
    @OperationLogAnnotation(module = "系统设置", action = "DELETE", description = "清除播放记录", targetType = "play_history")
    public Result<Void> clearPlayHistory() {
        playHistoryRepository.deleteAll();
        return Result.success();
    }

    @DeleteMapping("/search-history")
    @RequireAdmin
    @OperationLogAnnotation(module = "系统设置", action = "DELETE", description = "清除搜索记录", targetType = "search_history")
    public Result<Void> clearSearchHistory() {
        searchHistoryRepository.deleteAll();
        return Result.success();
    }
}
