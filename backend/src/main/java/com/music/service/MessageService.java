package com.music.service;

import com.music.entity.Conversation;
import com.music.entity.PrivateMessage;
import com.music.entity.User;
import com.music.entity.UserBlock;
import com.music.repository.ConversationRepository;
import com.music.repository.PrivateMessageRepository;
import com.music.repository.UserBlockRepository;
import com.music.repository.UserFollowRepository;
import com.music.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final PrivateMessageRepository privateMessageRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;

    private static final int STRANGER_MESSAGE_LIMIT = 2;

    @Transactional
    public PrivateMessage sendMessage(Long senderId, Long receiverId, String content) {
        if (senderId.equals(receiverId)) {
            throw new RuntimeException("不能给自己发私信");
        }

        if (userBlockRepository.existsByBlockerIdAndBlockedId(receiverId, senderId)) {
            throw new RuntimeException("对方已将你拉黑，无法发送私信");
        }

        if (userBlockRepository.existsByBlockerIdAndBlockedId(senderId, receiverId)) {
            throw new RuntimeException("你已拉黑对方，请先取消拉黑");
        }

        boolean isFollowing = userFollowRepository.existsByFollowerIdAndFollowingId(senderId, receiverId);
        if (!isFollowing) {
            long sentCount = privateMessageRepository.countSentMessages(senderId, receiverId);
            if (sentCount >= STRANGER_MESSAGE_LIMIT) {
                throw new RuntimeException("陌生人最多只能发送" + STRANGER_MESSAGE_LIMIT + "条私信，关注对方后可无限发送");
            }
        }

        Conversation conversation = conversationRepository.findByTwoUsers(senderId, receiverId)
                .orElseGet(() -> {
                    Conversation c = new Conversation();
                    c.setUser1Id(Math.min(senderId, receiverId));
                    c.setUser2Id(Math.max(senderId, receiverId));
                    c.setUser1Unread(0);
                    c.setUser2Unread(0);
                    return conversationRepository.save(c);
                });

        PrivateMessage message = new PrivateMessage();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        PrivateMessage saved = privateMessageRepository.save(message);

        conversation.setLastMessageId(saved.getId());
        conversation.setLastMessageContent(content.length() > 50 ? content.substring(0, 50) : content);
        conversation.setLastMessageTime(saved.getCreateTime());

        if (conversation.getUser1Id().equals(receiverId)) {
            conversation.setUser1Unread(conversation.getUser1Unread() + 1);
        } else {
            conversation.setUser2Unread(conversation.getUser2Unread() + 1);
        }
        conversationRepository.save(conversation);

        return saved;
    }

    public List<Conversation> getConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUserIdOrderByLastMessageTimeDesc(userId);
        for (Conversation c : conversations) {
            Long otherId = c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id();
            c.setOtherUserId(otherId);
            User other = userRepository.findById(otherId).orElse(null);
            if (other != null) {
                c.setOtherNickname(other.getNickname() != null ? other.getNickname() : other.getUsername());
                c.setOtherAvatar(other.getAvatar());
            }
        }
        return conversations;
    }

    public Page<PrivateMessage> getMessages(Long conversationId, Long userId, int page, int size) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new RuntimeException("无权访问该会话");
        }

        Page<PrivateMessage> messages = privateMessageRepository.findByConversationIdOrderByCreateTimeDesc(
                conversationId, PageRequest.of(page, size));

        for (PrivateMessage m : messages.getContent()) {
            User sender = userRepository.findById(m.getSenderId()).orElse(null);
            if (sender != null) {
                m.setSenderNickname(sender.getNickname() != null ? sender.getNickname() : sender.getUsername());
                m.setSenderAvatar(sender.getAvatar());
            }
        }

        return messages;
    }

    @Transactional
    public void markAsRead(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        privateMessageRepository.markAllAsRead(conversationId, userId);

        if (conversation.getUser1Id().equals(userId)) {
            conversation.setUser1Unread(0);
        } else {
            conversation.setUser2Unread(0);
        }
        conversationRepository.save(conversation);
    }

    public Map<String, Long> getUnreadCount(Long userId) {
        long count = 0;
        List<Conversation> conversations = conversationRepository.findByUserIdOrderByLastMessageTimeDesc(userId);
        for (Conversation c : conversations) {
            if (c.getUser1Id().equals(userId)) {
                count += c.getUser1Unread();
            } else {
                count += c.getUser2Unread();
            }
        }
        return Map.of("count", count);
    }

    public Conversation getOrCreateConversation(Long userId1, Long userId2) {
        return conversationRepository.findByTwoUsers(userId1, userId2)
                .orElseGet(() -> {
                    Conversation c = new Conversation();
                    c.setUser1Id(Math.min(userId1, userId2));
                    c.setUser2Id(Math.max(userId1, userId2));
                    c.setUser1Unread(0);
                    c.setUser2Unread(0);
                    return conversationRepository.save(c);
                });
    }

    @Transactional
    public void recallMessage(Long messageId, Long userId) {
        PrivateMessage message = privateMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("只能撤回自己的消息");
        }
        message.setIsRecalled(true);
        message.setContent("该消息已撤回");
        privateMessageRepository.save(message);
    }

    public boolean canSendMessage(Long senderId, Long receiverId) {
        if (userBlockRepository.existsByBlockerIdAndBlockedId(receiverId, senderId)) return false;
        if (userBlockRepository.existsByBlockerIdAndBlockedId(senderId, receiverId)) return false;
        boolean isFollowing = userFollowRepository.existsByFollowerIdAndFollowingId(senderId, receiverId);
        if (isFollowing) return true;
        long sentCount = privateMessageRepository.countSentMessages(senderId, receiverId);
        return sentCount < STRANGER_MESSAGE_LIMIT;
    }

    public int getRemainingStrangerMessages(Long senderId, Long receiverId) {
        boolean isFollowing = userFollowRepository.existsByFollowerIdAndFollowingId(senderId, receiverId);
        if (isFollowing) return -1;
        long sentCount = privateMessageRepository.countSentMessages(senderId, receiverId);
        return Math.max(0, STRANGER_MESSAGE_LIMIT - (int) sentCount);
    }
}
