package com.music.controller;

import com.music.common.Result;
import com.music.entity.Singer;
import com.music.entity.Song;
import com.music.service.SingerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/singers")
@RequiredArgsConstructor
public class SingerController {
    private final SingerService singerService;

    @GetMapping
    public Result<List<Singer>> getAllSingers() {
        return Result.success(singerService.getAllSingers());
    }

    @GetMapping("/{name}")
    public Result<Singer> getSingerByName(@PathVariable String name) {
        return Result.success(singerService.getSingerByName(name));
    }

    @GetMapping("/{name}/songs")
    public Result<List<Song>> getSingerSongs(@PathVariable String name) {
        return Result.success(singerService.getSingerSongs(name));
    }

    @GetMapping("/search")
    public Result<List<Singer>> searchSingers(@RequestParam String keyword) {
        return Result.success(singerService.searchSingers(keyword));
    }
}
