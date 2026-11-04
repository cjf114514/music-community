package com.music.controller;

import com.music.common.Result;
import com.music.entity.Song;
import com.music.service.FavoriteService;
import com.music.repository.SongRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;
    private final SongRepository songRepository;

    @PostMapping("/toggle")
    public Result<Void> toggleFavorite(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long songId = body.get("songId");
        favoriteService.toggleFavorite(userId, songId);
        return Result.success();
    }

    @GetMapping("/check/{songId}")
    public Result<Boolean> checkFavorite(@PathVariable Long songId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(favoriteService.isFavorite(userId, songId));
    }

    @GetMapping("/my")
    public Result<List<Song>> getMyFavorites(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(favoriteService.getMyFavorites(userId));
    }
}
