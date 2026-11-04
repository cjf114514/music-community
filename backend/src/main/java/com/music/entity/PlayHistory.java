package com.music.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "play_history")
public class PlayHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(name = "play_time", updatable = false)
    private LocalDateTime playTime;

    @PrePersist
    protected void onCreate() {
        playTime = LocalDateTime.now();
    }
}
