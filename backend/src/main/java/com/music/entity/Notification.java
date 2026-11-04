package com.music.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false) // 接收用户ID
    private Long userId;

    @Column(name = "actor_id", nullable = false) // 发送用户ID
    private Long actorId;

    @Column(nullable = false, length = 30) // 通知类型
    private String type;

    @Column(name = "target_id") // 关联的目标 ID（歌曲 / 评论 / 动态）
    private Long targetId;

    @Column(length = 500) // 通知内容
    private String content;

    @Column(name = "is_read", nullable = false) // 是否已读
    private Boolean isRead = false;

    @Column(name = "create_time", updatable = false) // 创建时间
    private LocalDateTime createTime;

    @PrePersist // 通知创建时设置创建时间为当前时间
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }

    @Transient // 临时字段（发送用户昵称），只用于前端展示，不存入数据库中
    private String actorNickname;

    @Transient // 临时字段（发送用户头像），只用于前端展示，不存入数据库中
    private String actorAvatar;
}
