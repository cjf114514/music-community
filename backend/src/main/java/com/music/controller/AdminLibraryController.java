package com.music.controller;

import com.music.annotation.RequireAdmin;
import com.music.common.Result;
import com.music.service.LibraryScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * 本地音乐库扫描 —— 管理端接口
 *
 * 用途：直接把硬盘上的音乐目录扫进数据库，替代「浏览器逐个上传」。
 * 音频文件不复制、不搬运，通过 /library/** 直接对外提供播放。
 *
 * GET  /api/admin/library/config  查看当前配置的目录、是否可用
 * POST /api/admin/library/scan    执行扫描（?dryRun=true 只统计不写入）
 */
@RestController
@RequestMapping("/api/admin/library")
@RequiredArgsConstructor
public class AdminLibraryController {

    private final LibraryScanService libraryScanService;

    @RequireAdmin
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        return Result.success(libraryScanService.config());
    }

    @RequireAdmin
    @PostMapping("/scan")
    public Result<Map<String, Object>> scan(@RequestParam(defaultValue = "false") boolean dryRun) throws IOException {
        return Result.success(libraryScanService.scan(dryRun));
    }
}
