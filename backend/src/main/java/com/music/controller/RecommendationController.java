package com.music.controller;

import com.music.common.Result;
import com.music.entity.Song;
import com.music.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendationService;

    @GetMapping("/daily")
    public Result<List<Song>> getDailyRecommendations(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(recommendationService.getDailyRecommendations(userId, limit));
    }

    @GetMapping("/taste")
    public Result<Map<String, Object>> getTasteAnalysis(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(recommendationService.getTasteAnalysis(userId));
    }

    @GetMapping("/similar/{songId}")
    public Result<List<Song>> getSimilarSongs(
            @PathVariable Long songId,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(recommendationService.getSimilarSongs(songId, userId, limit));
    }
}
