package com.music.controller;

import com.music.common.Result;
import com.music.entity.Song;
import com.music.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {
    private final SongService songService;

    @GetMapping("/{name}")
    public Result<List<Song>> getAlbumSongs(@PathVariable String name) {
        return Result.success(songService.getAlbumSongs(name));
    }
}
