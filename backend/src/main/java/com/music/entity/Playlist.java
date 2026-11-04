package com.music.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "playlist")
public class Playlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String category;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "is_public")
    private Integer isPublic;

    @Column(name = "song_count")
    private Integer songCount;

    @Column(name = "play_count")
    private Long playCount;

    private Integer status;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (isPublic == null) isPublic = 1;
        if (songCount == null) songCount = 0;
        if (playCount == null) playCount = 0L;
        if (status == null) status = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
