package com.music.repository;

import com.music.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE (c.user1Id = :userId OR c.user2Id = :userId) ORDER BY c.lastMessageTime DESC")
    List<Conversation> findByUserIdOrderByLastMessageTimeDesc(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE (c.user1Id = :u1 AND c.user2Id = :u2) OR (c.user1Id = :u2 AND c.user2Id = :u1)")
    Optional<Conversation> findByTwoUsers(@Param("u1") Long user1Id, @Param("u2") Long user2Id);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE (c.user1Id = :userId AND c.user1Unread > 0) OR (c.user2Id = :userId AND c.user2Unread > 0)")
    long countUnreadConversations(@Param("userId") Long userId);
}
