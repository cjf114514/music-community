package com.music.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "comment_like", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"comment_id", "user_id"})
})
public class CommentLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 主键，自增

    @Column(name = "comment_id", nullable = false)
    private Long commentId; // 评论ID

    @Column(name = "user_id", nullable = false)
    private Long userId; // 用户ID

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime; // 创建时间



    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
