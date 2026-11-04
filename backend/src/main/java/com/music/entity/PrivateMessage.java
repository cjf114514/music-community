package com.music.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "private_message")
public class PrivateMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "message_type", length = 20, nullable = false)
    private String messageType = "TEXT";

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "is_recalled", nullable = false)
    private Boolean isRecalled = false;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }

    @Transient
    private String senderNickname;

    @Transient
    private String senderAvatar;
}
