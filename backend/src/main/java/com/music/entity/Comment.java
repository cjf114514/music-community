package com.music.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "comment")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 评论ID

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "song_id")
    private Long songId;

    @Column(name = "playlist_id")
    private Long playlistId;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "timestamp")
    private Integer timestamp;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "like_count")
    private Integer likeCount;

    private Integer status;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (likeCount == null) likeCount = 0;
        if (status == null) status = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
