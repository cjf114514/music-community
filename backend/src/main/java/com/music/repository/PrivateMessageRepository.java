package com.music.repository;

import com.music.entity.PrivateMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {

    Page<PrivateMessage> findByConversationIdOrderByCreateTimeDesc(Long conversationId, Pageable pageable);

    long countBySenderIdAndReceiverIdAndIsReadFalse(Long senderId, Long receiverId);

    @Query("SELECT COUNT(m) FROM PrivateMessage m WHERE m.senderId = :senderId AND m.receiverId = :receiverId AND m.messageType = 'TEXT'")
    long countSentMessages(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    @Modifying
    @Query("UPDATE PrivateMessage m SET m.isRead = true WHERE m.conversationId = :conversationId AND m.receiverId = :receiverId AND m.isRead = false")
    void markAllAsRead(@Param("conversationId") Long conversationId, @Param("receiverId") Long receiverId);
}
