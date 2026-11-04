package com.music.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "search_history")
public class SearchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(name = "search_time", updatable = false)
    private LocalDateTime searchTime;

    @PrePersist
    protected void onCreate() {
        searchTime = LocalDateTime.now();
    }
}
