package com.music.repository;

import com.music.entity.SongProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SongProgressRepository extends JpaRepository<SongProgress, Long> {

    /** 某个用户在某首歌上的进度 */
    Optional<SongProgress> findByUserIdAndSongId(Long userId, Long songId);

    /** 某个用户最近一次听的是什么（用于「继续上次播放」） */
    Optional<SongProgress> findFirstByUserIdOrderByUpdatedAtDesc(Long userId);

    void deleteByUserIdAndSongId(Long userId, Long songId);

    void deleteByUserId(Long userId);
}
