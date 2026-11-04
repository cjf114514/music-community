package com.music.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "playlist_song")
public class PlaylistSong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (sortOrder == null) sortOrder = 0;
    }
}
