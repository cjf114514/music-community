package com.music.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 歌曲播放进度 —— 支持「断点续播」与「跨设备续听」
 *
 * 为什么不复用 play_history？
 *   play_history 语义是「播放事件流水」，一行代表一次播放，用来做统计与排行榜；
 *   而进度是「每首歌一条最新状态」，需要按 (user, song) 唯一并反复更新。
 *   两者混在一起会让统计被反复写坏，所以单独建表。
 *
 * 唯一键 (user_id, song_id) 保证一个用户一首歌只有一条进度记录。
 */
@Data
@Entity
@Table(
        name = "song_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_progress_user_song", columnNames = {"user_id", "song_id"})
)
public class SongProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    /** 上次播放到的位置（秒） */
    @Column(name = "position_seconds", nullable = false)
    private Integer positionSeconds;

    /** 最后一次更新进度的时间，用于「继续上次播放」取最近一条 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }
}
