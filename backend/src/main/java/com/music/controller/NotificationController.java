package com.music.controller;

import com.music.common.Result;
import com.music.entity.Notification;
import com.music.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public Result<List<Notification>> getNotifications(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(notificationService.getNotifications(userId));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    @PostMapping("/read-all")
    public Result<Void> markAllAsRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        notificationService.markAllAsRead(userId);
        return Result.success();
    }

    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        notificationService.markAsRead(id, userId);
        return Result.success();
    }
}
