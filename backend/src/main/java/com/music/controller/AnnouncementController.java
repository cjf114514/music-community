package com.music.controller;

import com.music.common.Result;
import com.music.entity.Announcement;
import com.music.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public Result<Map<String, Object>> getPublishedAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Announcement> announcementPage = announcementService.getAnnouncements(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime")));

        java.util.List<Announcement> published = announcementPage.getContent().stream()
                .filter(a -> "published".equals(a.getStatus()))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("total", announcementPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("list", published);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Announcement> getAnnouncement(@PathVariable Long id) {
        Announcement a = announcementService.getAnnouncement(id);
        if (!"published".equals(a.getStatus())) {
            throw new RuntimeException("公告不存在");
        }
        return Result.success(a);
    }
}
