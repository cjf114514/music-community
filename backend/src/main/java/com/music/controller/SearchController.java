package com.music.controller;

import com.music.common.Result;
import com.music.entity.User;
import com.music.repository.UserRepository;
import com.music.service.SearchHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchHistoryService searchHistoryService;
    private final UserRepository userRepository;

    @PostMapping("/history")
    public Result<Void> recordSearch(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String keyword = body.get("keyword");
        searchHistoryService.recordSearch(userId, keyword);
        return Result.success();
    }

    @GetMapping("/history")
    public Result<List<String>> getHistory(@RequestParam(defaultValue = "10") int limit, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(searchHistoryService.getHistory(userId, limit));
    }

    @DeleteMapping("/history")
    public Result<Void> deleteHistory(@RequestParam String keyword, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        searchHistoryService.deleteHistory(userId, keyword);
        return Result.success();
    }

    @DeleteMapping("/history/clear")
    public Result<Void> clearHistory(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        searchHistoryService.clearHistory(userId);
        return Result.success();
    }

    @GetMapping("/hot")
    public Result<List<String>> getHotSearch(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(searchHistoryService.getHotSearch(limit));
    }

    @GetMapping("/users")
    public Result<Map<String, Object>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = null;
        Page<User> userPage = userRepository.searchUsers(keyword, PageRequest.of(page, size));
        List<User> users = userPage.getContent();
        // password 已在 User.password 上用 @JsonIgnore 屏蔽,不再需要手动置空。
        // 原先的 users.forEach(u -> u.setPassword(null)) 已移除:
        // 它改的是实体对象,一旦此方法被纳入事务,脏检查会把数据库里的密码写成 NULL。
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("list", users);
        result.put("total", userPage.getTotalElements());
        result.put("pages", userPage.getTotalPages());
        return Result.success(result);
    }
}
