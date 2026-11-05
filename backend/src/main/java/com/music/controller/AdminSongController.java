package com.music.controller;

import com.music.annotation.OperationLogAnnotation;
import com.music.annotation.RequireAdmin;
import com.music.common.Result;
import com.music.entity.Song;
import com.music.repository.SongRepository;
import com.music.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/songs")
@RequiredArgsConstructor
public class AdminSongController {

    private final SongRepository songRepository;
    private final SongService songService;

    @GetMapping
    @RequireAdmin
    public Result<Map<String, Object>> getSongs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String singer,
            @RequestParam(required = false) Integer status) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Song> songPage;
        
        if (keyword != null && !keyword.isEmpty()) {
            songPage = songRepository.findByNameContaining(keyword, pageable);
        } else if (singer != null && !singer.isEmpty()) {
            songPage = songRepository.findBySingerContaining(singer, pageable);
        } else {
            songPage = songRepository.findAll(pageable);
        }
        
        List<Song> songs = songPage.getContent();
        if (status != null) {
            songs = songs.stream()
                    .filter(s -> status.equals(s.getStatus()))
                    .toList();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", songPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("list", songs);
        
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @RequireAdmin
    public Result<Song> getSong(@PathVariable Long id) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("歌曲不存在"));
        return Result.success(song);
    }

    @PutMapping("/{id}")
    @RequireAdmin
    @OperationLogAnnotation(module = "歌曲管理", action = "UPDATE", description = "编辑歌曲", targetType = "song")
    public Result<Song> updateSong(@PathVariable Long id, @RequestBody Song songInfo) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("歌曲不存在"));
        
        if (songInfo.getName() != null) song.setName(songInfo.getName());
        if (songInfo.getSinger() != null) song.setSinger(songInfo.getSinger());
        if (songInfo.getAlbum() != null) song.setAlbum(songInfo.getAlbum());
        if (songInfo.getUrl() != null) song.setUrl(songInfo.getUrl());
        if (songInfo.getCoverUrl() != null) song.setCoverUrl(songInfo.getCoverUrl());
        if (songInfo.getLyric() != null) song.setLyric(songInfo.getLyric());
        if (songInfo.getDuration() != null) song.setDuration(songInfo.getDuration());
        if (songInfo.getStatus() != null) song.setStatus(songInfo.getStatus());
        
        return Result.success(songRepository.save(song));
    }

    @PutMapping("/{id}/status")
    @RequireAdmin
    @OperationLogAnnotation(module = "歌曲管理", action = "UPDATE", description = "修改歌曲状态", targetType = "song")
    public Result<Void> updateSongStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("歌曲不存在"));
        
        Integer status = params.get("status");
        if (status != 0 && status != 1) {
            throw new RuntimeException("无效的状态");
        }
        
        song.setStatus(status);
        songRepository.save(song);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @OperationLogAnnotation(module = "歌曲管理", action = "DELETE", description = "删除歌曲", targetType = "song")
    public Result<Void> deleteSong(@PathVariable Long id) {
        if (!songRepository.existsById(id)) {
            throw new RuntimeException("歌曲不存在");
        }
        songRepository.deleteById(id);
        return Result.success();
    }

    @PostMapping
    @RequireAdmin
    @OperationLogAnnotation(module = "歌曲管理", action = "CREATE", description = "新增歌曲", targetType = "song")
    public Result<Song> createSong(@RequestBody Song song) {
        if (song.getName() == null || song.getName().isEmpty()) {
            throw new RuntimeException("歌曲名称不能为空");
        }
        song.setStatus(1);
        song.setPlayCount(0L);
        return Result.success(songRepository.save(song));
    }

    /**
     * 批量修改歌曲标签 —— PUT /api/admin/songs/batch
     *
     * 请求体示例：
     *   { "ids": [1,2,3], "genre": "纯音乐", "album": "合集", "singer": "某某", "status": 1 }
     *
     * 规则：只处理出现在请求体里的字段；
     *      传空字符串表示"清空该字段"（比如把错误的专辑名去掉），
     *      不传则保持原值不变。
     */
    @PutMapping("/batch")
    @RequireAdmin
    @OperationLogAnnotation(module = "歌曲管理", action = "UPDATE", description = "批量编辑歌曲", targetType = "song")
    public Result<Map<String, Object>> batchUpdate(@RequestBody Map<String, Object> body) {
        Object rawIds = body.get("ids");
        if (!(rawIds instanceof List<?> idList) || idList.isEmpty()) {
            return Result.error(400, "请选择要修改的歌曲");
        }
        List<Long> ids = new ArrayList<>();
        for (Object o : idList) {
            if (o != null) ids.add(Long.valueOf(o.toString()));
        }

        Map<String, Object> fields = new HashMap<>();
        for (String key : List.of("genre", "album", "singer", "status")) {
            if (body.containsKey(key)) fields.put(key, body.get(key));
        }
        if (fields.isEmpty()) {
            return Result.error(400, "没有要修改的字段");
        }

        int changed = songService.batchUpdate(ids, fields);
        Map<String, Object> result = new HashMap<>();
        result.put("changed", changed);
        result.put("requested", ids.size());
        return Result.success(result);
    }
}
