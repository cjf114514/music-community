package com.music.controller;

import com.music.annotation.OperationLogAnnotation;
import com.music.annotation.RequireAdmin;
import com.music.common.Result;
import com.music.entity.Playlist;
import com.music.entity.User;
import com.music.repository.PlaylistRepository;
import com.music.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/playlists")
@RequiredArgsConstructor
public class AdminPlaylistController {

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;

    @GetMapping
    @RequireAdmin
    public Result<Map<String, Object>> getPlaylists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        
        List<Playlist> allPlaylists = playlistRepository.findAll();
        
        List<Playlist> playlists = allPlaylists.stream()
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .toList();
        
        if (keyword != null && !keyword.isEmpty()) {
            playlists = playlists.stream()
                    .filter(p -> p.getName().contains(keyword) || 
                            (p.getDescription() != null && p.getDescription().contains(keyword)))
                    .toList();
        }
        
        if (status != null) {
            playlists = playlists.stream()
                    .filter(p -> status.equals(p.getStatus()))
                    .toList();
        }
        
        int total = playlists.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);
        
        List<Playlist> pagePlaylists = fromIndex < total ? playlists.subList(fromIndex, toIndex) : List.of();
        
        List<Map<String, Object>> playlistList = pagePlaylists.stream().map(playlist -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", playlist.getId());
            map.put("name", playlist.getName());
            map.put("coverUrl", playlist.getCoverUrl());
            map.put("description", playlist.getDescription());
            map.put("userId", playlist.getUserId());
            map.put("isPublic", playlist.getIsPublic());
            map.put("category", playlist.getCategory());
            map.put("songCount", playlist.getSongCount());
            map.put("playCount", playlist.getPlayCount());
            map.put("status", playlist.getStatus());
            map.put("createTime", playlist.getCreateTime());
            
            User user = userRepository.findById(playlist.getUserId()).orElse(null);
            if (user != null) {
                map.put("username", user.getUsername());
                map.put("nickname", user.getNickname());
            }
            
            return map;
        }).toList();
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("list", playlistList);
        
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    @RequireAdmin
    @OperationLogAnnotation(module = "歌单管理", action = "UPDATE", description = "修改歌单状态", targetType = "playlist")
    public Result<Void> updatePlaylistStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("歌单不存在"));
        
        Integer status = params.get("status");
        if (status != 0 && status != 1) {
            throw new RuntimeException("无效的状态");
        }
        
        playlist.setStatus(status);
        playlistRepository.save(playlist);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @OperationLogAnnotation(module = "歌单管理", action = "DELETE", description = "删除歌单", targetType = "playlist")
    public Result<Void> deletePlaylist(@PathVariable Long id) {
        if (!playlistRepository.existsById(id)) {
            throw new RuntimeException("歌单不存在");
        }
        playlistRepository.deleteById(id);
        return Result.success();
    }
}
