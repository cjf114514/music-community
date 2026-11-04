package com.music.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "song_share")
public class SongShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "share_token", unique = true, length = 100)
    private String shareToken;

    @Column(name = "share_count", nullable = false)
    private Integer shareCount;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (shareCount == null) shareCount = 0;
    }
}
