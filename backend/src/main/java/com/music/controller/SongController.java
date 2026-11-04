package com.music.controller;

import com.music.common.Result;
import com.music.entity.Song;
import com.music.service.SongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {
    private final SongService songService;

    /** 上传根目录,从 application.yml 读取 (如: ./uploads) */
    @Value("${app.upload-dir}")
    private String uploadDir;

    /**
     * LRC 时间标签，如 [00:12] / [01:23.45]
     * 用来判断 lyric 字段存的是「歌词正文」还是「旧数据的文件路径」
     */
    private static final java.util.regex.Pattern LRC_TIME_TAG =
            java.util.regex.Pattern.compile("\\[\\d{1,2}:\\d{2}");

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
     * lyric 字段支持两种存法，这里都要兼容：
     *
     *   ① 存歌词正文（当前方式）
     *      直接把 LRC 文本存进数据库 TEXT 列。
     *      好处：不依赖磁盘文件，不会出现"文件被删了歌词就没了"，
     *            导出数据库时歌词一起带走，编辑时直接改文本。
     *
     *   ② 存文件路径（历史方式，如 /uploads/lyric/xxx.lrc）
     *      早期设计是把 .lrc 传到 uploads/lyric 下，库里只存路径，
     *      读的时候再打开文件。问题是一旦文件丢失就永久读不到（而且没有清理机制）。
     *      保留这个分支只为兼容已存在的旧数据。
     *
     * 判断依据：内容里出现 [mm:ss 这种 LRC 时间标签，就当成正文；否则按路径处理。
     */
    @GetMapping("/{id}/lyric")
    public Result<String> getLyric(@PathVariable Long id) {
        Song song = songService.getSongById(id);
        if (song == null || song.getLyric() == null || song.getLyric().isBlank()) {
            return Result.success(null);
        }
        String lyric = song.getLyric();

        // ① 正文：含 [mm:ss 时间标签 → 直接返回
        if (LRC_TIME_TAG.matcher(lyric).find()) {
            return Result.success(lyric);
        }

        // ② 旧数据的文件路径：读文件返回
        try {
            String fileName = lyric.substring(lyric.lastIndexOf("/") + 1);
            String lyricPath = uploadDir + "/lyric/" + fileName;
            String content = Files.readString(Paths.get(lyricPath));
            return Result.success(content);
        } catch (IOException e) {
            log.warn("歌词文件读取失败，songId={}, lyric={}", id, lyric);
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
