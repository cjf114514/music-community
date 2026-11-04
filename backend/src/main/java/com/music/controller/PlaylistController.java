package com.music.controller;

import com.music.common.Result;
import com.music.entity.Playlist;
import com.music.entity.Song;
import com.music.exception.ResourceNotFoundException;
import com.music.service.PlaylistService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.music.dto.ReorderRequest;
import java.util.List;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {
    private final PlaylistService playlistService;

    @GetMapping
    public Result<List<Playlist>> getPublicPlaylists(@RequestParam(required = false) String category) {
        return Result.success(playlistService.getPublicPlaylistsByCategory(category));
    }

    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        return Result.success(playlistService.getCategories());
    }

    @GetMapping("/search")
    public Result<List<Playlist>> searchPlaylists(@RequestParam String keyword) {
        return Result.success(playlistService.searchPlaylists(keyword));
    }

    @GetMapping("/user/{userId}")
    public Result<List<Playlist>> getUserPlaylists(@PathVariable Long userId) {
        return Result.success(playlistService.getUserPlaylists(userId));
    }

    @GetMapping("/{id}")
    public Result<Playlist> getPlaylistById(@PathVariable Long id) {
        Playlist playlist = playlistService.getPlaylistById(id);
        if (playlist == null) {
            // 歌单被删除 / 链接过期 → 404，前端会显示「歌单不存在或已被删除」
            throw new ResourceNotFoundException("歌单不存在或已被删除");
        }
        return Result.success(playlist);
    }

    @GetMapping("/{id}/songs")
    public Result<List<Song>> getPlaylistSongs(@PathVariable Long id) {
        return Result.success(playlistService.getPlaylistSongs(id));
    }

    @PostMapping
    public Result<Playlist> createPlaylist(@RequestBody Playlist playlist, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Playlist saved = playlistService.savePlaylist(playlist);
        return Result.success(saved);
    }

    @PutMapping("/{id}")
    public Result<Playlist> updatePlaylist(@PathVariable Long id, @RequestBody Playlist playlist) {
        Playlist existing = playlistService.getPlaylistById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("歌单不存在");
        }
        if (playlist.getName() != null) existing.setName(playlist.getName());
        if (playlist.getCoverUrl() != null) existing.setCoverUrl(playlist.getCoverUrl());
        if (playlist.getDescription() != null) existing.setDescription(playlist.getDescription());
        if (playlist.getIsPublic() != null) existing.setIsPublic(playlist.getIsPublic());
        if (playlist.getCategory() != null) existing.setCategory(playlist.getCategory());
        return Result.success(playlistService.savePlaylist(existing));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePlaylist(@PathVariable Long id) {
        playlistService.deletePlaylist(id);
        return Result.success();
    }

    @PostMapping("/{playlistId}/songs/{songId}")
    public Result<Void> addSongToPlaylist(@PathVariable Long playlistId, @PathVariable Long songId) {
        playlistService.addSongToPlaylist(playlistId, songId);
        return Result.success();
    }

    @DeleteMapping("/{playlistId}/songs/{songId}")
    public Result<Void> removeSongFromPlaylist(@PathVariable Long playlistId, @PathVariable Long songId) {
        playlistService.removeSongFromPlaylist(playlistId, songId);
        return Result.success();
    }

    @PutMapping("/{playlistId}/songs/reorder")
    public Result<Void> reorderSongs(@PathVariable Long playlistId, @RequestBody ReorderRequest body) {
        List<Long> songIds = body.getSongIds();
        if (songIds == null || songIds.isEmpty()) {
            return Result.error("歌曲ID列表不能为空");
        }
        playlistService.reorderSongs(playlistId, songIds);
        return Result.success();
    }
}
