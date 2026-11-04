package com.music.controller;

import com.music.common.Result;
import com.music.entity.Song;
import com.music.service.PlayHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/play-history")
@RequiredArgsConstructor
public class PlayHistoryController {
    private final PlayHistoryService playHistoryService;

    @PostMapping("/record")
    public Result<Void> recordPlay(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long songId = body.get("songId");
        playHistoryService.recordPlay(userId, songId);
        return Result.success();
    }

    @GetMapping("/recent")
    public Result<List<Song>> getRecentPlays(@RequestParam(defaultValue = "50") int limit, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(playHistoryService.getRecentPlays(userId, limit));
    }

    @DeleteMapping("/clear")
    public Result<Void> clearHistory(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        playHistoryService.clearHistory(userId);
        return Result.success();
    }

    @GetMapping("/ranking")
    public Result<List<Song>> getRanking(@RequestParam(defaultValue = "total") String type,
                                         @RequestParam(defaultValue = "20") int limit) {
        return Result.success(playHistoryService.getRanking(type, limit));
    }
}
