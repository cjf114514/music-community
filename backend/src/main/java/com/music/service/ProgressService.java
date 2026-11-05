package com.music.service;

import com.music.entity.Song;
import com.music.entity.SongProgress;
import com.music.repository.SongProgressRepository;
import com.music.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 播放进度服务 —— 断点续播 / 跨设备续听
 *
 * 前端在播放时每 10 秒上报一次位置（带节流），后端按 (user, song) 覆盖更新；
 * 换设备登录同一账号时读到的是同一个位置，所以能"接着听"。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final SongProgressRepository progressRepository;
    private final SongRepository songRepository;

    /** 小于这个秒数视为"从头开始"，不保存（避免刚点开就留下一条噪音记录） */
    private static final int MIN_SAVE_SECONDS = 5;

    /**
     * 保存播放位置
     *
     * @param positionSeconds 传 0 或很小的值表示"结束了/要重头听" → 删除已有进度
     */
    @Transactional
    public void savePosition(Long userId, Long songId, int positionSeconds) {
        if (positionSeconds < MIN_SAVE_SECONDS) {
            // 从头播放或播放结束：清掉进度，下次就该从头开始
            progressRepository.deleteByUserIdAndSongId(userId, songId);
            return;
        }
        SongProgress progress = progressRepository.findByUserIdAndSongId(userId, songId)
                .orElseGet(() -> {
                    SongProgress p = new SongProgress();
                    p.setUserId(userId);
                    p.setSongId(songId);
                    return p;
                });
        progress.setPositionSeconds(positionSeconds);
        progressRepository.save(progress);
    }

    /** 取某首歌的进度，没有则返回 0 */
    public int getPosition(Long userId, Long songId) {
        return progressRepository.findByUserIdAndSongId(userId, songId)
                .map(SongProgress::getPositionSeconds)
                .orElse(0);
    }

    /**
     * 继续上次播放：返回最近一条进度对应的歌曲与位置
     *
     * 返回 null 表示没有可续播的记录（新用户 / 从未播放过）。
     */
    public Map<String, Object> resume(Long userId) {
        Optional<SongProgress> latest = progressRepository.findFirstByUserIdOrderByUpdatedAtDesc(userId);
        if (latest.isEmpty()) return null;

        SongProgress progress = latest.get();
        Song song = songRepository.findById(progress.getSongId()).orElse(null);
        if (song == null || song.getStatus() == null || song.getStatus() != 1) {
            // 歌曲已被删除或下架，这条进度没意义了
            progressRepository.deleteByUserIdAndSongId(userId, progress.getSongId());
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("song", song);                       // 歌词已从实体序列化中剥离，不会带出来
        result.put("position", progress.getPositionSeconds());
        result.put("updatedAt", progress.getUpdatedAt());
        return result;
    }

    @Transactional
    public void clear(Long userId, Long songId) {
        progressRepository.deleteByUserIdAndSongId(userId, songId);
    }

    @Transactional
    public void clearAll(Long userId) {
        progressRepository.deleteByUserId(userId);
    }
}
