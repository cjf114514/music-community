package com.music.controller;

import com.music.annotation.OperationLogAnnotation;
import com.music.annotation.RequireAdmin;
import com.music.common.Result;
import com.music.entity.Announcement;
import com.music.service.AnnouncementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    @RequireAdmin
    public Result<Map<String, Object>> getAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Announcement> announcementPage = announcementService.getAnnouncements(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime")));

        Map<String, Object> result = new HashMap<>();
        result.put("total", announcementPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("list", announcementPage.getContent());
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @RequireAdmin
    public Result<Announcement> getAnnouncement(@PathVariable Long id) {
        return Result.success(announcementService.getAnnouncement(id));
    }

    @PostMapping
    @RequireAdmin
    @OperationLogAnnotation(module = "公告管理", action = "CREATE", description = "发布公告", targetType = "announcement")
    public Result<Announcement> createAnnouncement(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        String type = (String) params.get("type");
        Boolean isTop = params.get("isTop") != null ? (Boolean) params.get("isTop") : false;
        return Result.success(announcementService.createAnnouncement(title, content, type, isTop, adminId));
    }

    @PutMapping("/{id}")
    @RequireAdmin
    @OperationLogAnnotation(module = "公告管理", action = "UPDATE", description = "编辑公告", targetType = "announcement")
    public Result<Announcement> updateAnnouncement(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        String type = (String) params.get("type");
        Boolean isTop = params.get("isTop") != null ? (Boolean) params.get("isTop") : null;
        return Result.success(announcementService.updateAnnouncement(id, title, content, type, isTop));
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @OperationLogAnnotation(module = "公告管理", action = "DELETE", description = "删除公告", targetType = "announcement")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return Result.success();
    }

    @PutMapping("/{id}/toggle-status")
    @RequireAdmin
    @OperationLogAnnotation(module = "公告管理", action = "UPDATE", description = "切换公告状态", targetType = "announcement")
    public Result<Announcement> toggleStatus(@PathVariable Long id) {
        return Result.success(announcementService.toggleStatus(id));
    }
}
