package com.music.controller;

import com.music.common.Result;
import com.music.entity.Song;
import com.music.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 歌曲控制器 — 歌曲的查询、搜索、增删改、下载
 *
 * 所有接口路径以 /api/songs 开头
 * GET 查询接口在白名单中 → 游客可访问
 * POST/PUT/DELETE 需要登录 → JwtInterceptor 会拦截
 */
@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {
    private final SongService songService;

    /** 上传根目录,从 application.yml 读取 (如: ./uploads) */
    @Value("${app.upload-dir}")
    private String uploadDir;

    /** 获取所有歌曲 — GET /api/songs */
    @GetMapping
    public Result<List<Song>> getAllSongs() {
        return Result.success(songService.getAllSongs());
    }

    /**
     * 歌曲详情 — GET /api/songs/1
     * @PathVariable: URL 中的 {id} → 方法参数 id
     */
    @GetMapping("/{id}")
    public Result<Song> getSongById(@PathVariable Long id) {
        return Result.success(songService.getSongById(id));
    }

    /**
     * 歌词内容 — GET /api/songs/1/lyric
     *
     * 歌词存储方式: lyric 字段存文件路径 → 读文件内容 → 返回 LRC 文本
     * 为什么存文件而不是直接存数据库?
     *   LRC 歌词可能很长 → TEXT 列有大小限制 → 单独文件更灵活
     */
    @GetMapping("/{id}/lyric")
    public Result<String> getLyric(@PathVariable Long id) {
        Song song = songService.getSongById(id);
        if (song == null || song.getLyric() == null) {
            return Result.success(null);
        }
        try {
            String fileName = song.getLyric().substring(song.getLyric().lastIndexOf("/") + 1);
            String lyricPath = uploadDir + "/lyric/" + fileName;
            String content = Files.readString(Paths.get(lyricPath));
            return Result.success(content);
        } catch (IOException e) {
            return Result.success(null);
        }
    }

    /**
     * 搜索 — GET /api/songs/search?keyword=周杰伦
     * @RequestParam: URL 查询参数 → 方法参数
     */
    @GetMapping("/search")
    public Result<List<Song>> searchSongs(@RequestParam String keyword) {
        return Result.success(songService.searchAll(keyword));
    }

    /**
     * 随机 — GET /api/songs/random?limit=10
     * defaultValue: 前端没传 limit 时默认为 10
     */
    @GetMapping("/random")
    public Result<List<Song>> getRandomSongs(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(songService.getRandomSongs(limit));
    }

    /** 添加歌曲 — POST /api/songs (需要登录) */
    @PostMapping
    public Result<Song> addSong(@RequestBody Song song) {
        return Result.success(songService.saveSong(song));
    }

    /** 删除 — DELETE /api/songs/1 (需要登录) */
    @DeleteMapping("/{id}")
    public Result<Void> deleteSong(@PathVariable Long id) {
        songService.deleteSong(id);
        return Result.success();
    }

    /**
     * 更新歌曲 — PUT /api/songs/1 (需要登录)
     * 按需更新: 只更新非 null 字段,没传的字段保持原值不变
     */
    @PutMapping("/{id}")
    public Result<Song> updateSong(@PathVariable Long id, @RequestBody Song song) {
        Song existing = songService.getSongById(id);
        if (existing == null) {
            return Result.error("歌曲不存在");
        }
        if (song.getName() != null) existing.setName(song.getName());
        if (song.getSinger() != null) existing.setSinger(song.getSinger());
        if (song.getAlbum() != null) existing.setAlbum(song.getAlbum());
        if (song.getUrl() != null) existing.setUrl(song.getUrl());
        if (song.getCoverUrl() != null) existing.setCoverUrl(song.getCoverUrl());
        if (song.getLyric() != null) existing.setLyric(song.getLyric());
        if (song.getDuration() != null) existing.setDuration(song.getDuration());
        if (song.getFileSize() != null) existing.setFileSize(song.getFileSize());
        if (song.getFormat() != null) existing.setFormat(song.getFormat());
        if (song.getGenre() != null) existing.setGenre(song.getGenre());
        return Result.success(songService.saveSong(existing));
    }

    /**
     * 更新时长 — PATCH /api/songs/1/duration
     * PATCH vs PUT:
     *   PUT → 完整更新 (传整个对象)
     *   PATCH → 局部更新 (只改一个字段)
     */
    @PatchMapping("/{id}/duration")
    public Result<Void> updateDuration(@PathVariable Long id, @RequestBody java.util.Map<String, Long> body) {
        Long duration = body.get("duration");
        if (duration == null || duration <= 0) {
            return Result.error("无效的时长");
        }
        Song existing = songService.getSongById(id);
        if (existing == null) {
            return Result.error("歌曲不存在");
        }
        existing.setDuration(duration);
        songService.saveSong(existing);
        return Result.success();
    }

    /**
     * 文件下载 — GET /api/songs/1/download
     *
     * ResponseEntity<Resource> → 不是 JSON 响应,是文件下载响应
     * Content-Disposition: attachment → 浏览器触发下载
     * URLEncoder: 中文文件名编码,否则浏览器识别不了
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadSong(@PathVariable Long id) {
        Song song = songService.getSongById(id);
        if (song == null || song.getUrl() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            String fileName = song.getUrl().substring(song.getUrl().lastIndexOf("/") + 1);
            String filePath = uploadDir + "/audio/" + fileName;
            Resource resource = new FileSystemResource(filePath);
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            String downloadName = URLEncoder.encode(
                    song.getName() + " - " + song.getSinger() + "." + song.getFormat(),
                    StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
