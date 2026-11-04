package com.music.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "conversation")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "last_message_content", length = 500)
    private String lastMessageContent;

    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;

    @Column(name = "user1_unread", nullable = false)
    private Integer user1Unread = 0;

    @Column(name = "user2_unread", nullable = false)
    private Integer user2Unread = 0;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    @Transient
    private String otherNickname;

    @Transient
    private String otherAvatar;

    @Transient
    private Long otherUserId;
}
