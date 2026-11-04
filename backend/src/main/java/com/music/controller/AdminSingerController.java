package com.music.controller;

import com.music.annotation.RequireAdmin;
import com.music.common.Result;
import com.music.entity.Singer;
import com.music.entity.Song;
import com.music.repository.SingerRepository;
import com.music.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/singers")
@RequiredArgsConstructor
public class AdminSingerController {

    private final SingerRepository singerRepository;
    private final SongRepository songRepository;

    @GetMapping
    @RequireAdmin
    public Result<Map<String, Object>> getSingers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "songCount"));
        Page<Singer> singerPage;

        if (keyword != null && !keyword.isEmpty()) {
            singerPage = singerRepository.findByNameContaining(keyword, pageable);
        } else {
            singerPage = singerRepository.findAll(pageable);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", singerPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("list", singerPage.getContent());

        return Result.success(result);
    }

    @GetMapping("/{id}")
    @RequireAdmin
    public Result<Singer> getSinger(@PathVariable Long id) {
        Singer singer = singerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("歌手不存在"));
        return Result.success(singer);
    }

    @PostMapping
    @RequireAdmin
    public Result<Singer> createSinger(@RequestBody Singer singer) {
        if (singer.getName() == null || singer.getName().isEmpty()) {
            throw new RuntimeException("歌手名称不能为空");
        }

        singerRepository.findByName(singer.getName()).ifPresent(s -> {
            throw new RuntimeException("歌手已存在");
        });

        if (singer.getSongCount() == null) singer.setSongCount(0);
        return Result.success(singerRepository.save(singer));
    }

    @PutMapping("/{id}")
    @RequireAdmin
    public Result<Singer> updateSinger(@PathVariable Long id, @RequestBody Singer singerInfo) {
        Singer singer = singerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("歌手不存在"));

        if (singerInfo.getName() != null) singer.setName(singerInfo.getName());
        if (singerInfo.getAvatar() != null) singer.setAvatar(singerInfo.getAvatar());
        if (singerInfo.getDescription() != null) singer.setDescription(singerInfo.getDescription());

        return Result.success(singerRepository.save(singer));
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    public Result<Void> deleteSinger(@PathVariable Long id) {
        if (!singerRepository.existsById(id)) {
            throw new RuntimeException("歌手不存在");
        }
        singerRepository.deleteById(id);
        return Result.success();
    }

    @PutMapping("/{id}/sync-song-count")
    @RequireAdmin
    public Result<Void> syncSongCount(@PathVariable Long id) {
        Singer singer = singerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("歌手不存在"));

        long count = songRepository.countBySinger(singer.getName());
        singer.setSongCount((int) count);
        singerRepository.save(singer);
        return Result.success();
    }
}
