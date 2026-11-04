package com.music.controller;

import com.music.common.Result;
import com.music.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;

    @PostMapping("/audio")
    public Result<String> uploadAudio(@RequestParam("file") MultipartFile file) {
        validateAudioFile(file);
        String url = storageService.upload(file, "audio");
        return Result.success(url);
    }

    @PostMapping("/cover")
    public Result<String> uploadCover(@RequestParam("file") MultipartFile file) {
        validateImageFile(file);
        String url = storageService.upload(file, "cover");
        return Result.success(url);
    }

    @PostMapping("/lyric")
    public Result<String> uploadLyric(@RequestParam("file") MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".lrc")) {
            return Result.error("仅支持 .lrc 格式的歌词文件");
        }
        String url = storageService.upload(file, "lyric");
        return Result.success(url);
    }

    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        validateImageFile(file);
        String url = storageService.upload(file, "avatar");
        return Result.success(url);
    }

    @PostMapping("/post-image")
    public Result<String> uploadPostImage(@RequestParam("file") MultipartFile file) {
        validateImageFile(file);
        String url = storageService.upload(file, "post-image");
        return Result.success(url);
    }

    private void validateAudioFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("无法获取文件名");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        String[] allowedAudio = {".mp3", ".wav", ".flac", ".aac", ".ogg", ".m4a"};
        for (String ext : allowedAudio) {
            if (ext.equals(extension)) return;
        }
        throw new RuntimeException("不支持的音频格式，仅支持: mp3, wav, flac, aac, ogg, m4a");
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("无法获取文件名");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        String[] allowedImage = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
        for (String ext : allowedImage) {
            if (ext.equals(extension)) return;
        }
        throw new RuntimeException("不支持的图片格式，仅支持: jpg, jpeg, png, gif, webp");
    }
}
