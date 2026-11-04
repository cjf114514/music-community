package com.music.controller;

import com.music.common.Result;
import com.music.entity.Conversation;
import com.music.entity.PrivateMessage;
import com.music.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/conversations")
    public Result<List<Conversation>> getConversations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageService.getConversations(userId));
    }

    @GetMapping("/conversation/{userId}")
    public Result<Conversation> getOrCreateConversation(@PathVariable Long userId, HttpServletRequest request) {
        Long myId = (Long) request.getAttribute("userId");
        return Result.success(messageService.getOrCreateConversation(myId, userId));
    }

    @GetMapping("/{conversationId}")
    public Result<Page<PrivateMessage>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageService.getMessages(conversationId, userId, page, size));
    }

    @PostMapping("/send")
    public Result<PrivateMessage> sendMessage(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long senderId = (Long) request.getAttribute("userId");
        Long receiverId = Long.valueOf(params.get("receiverId").toString());
        String content = params.get("content").toString();
        return Result.success(messageService.sendMessage(senderId, receiverId, content));
    }

    @PostMapping("/{conversationId}/read")
    public Result<Void> markAsRead(@PathVariable Long conversationId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        messageService.markAsRead(conversationId, userId);
        return Result.success();
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageService.getUnreadCount(userId));
    }

    @PostMapping("/{messageId}/recall")
    public Result<Void> recallMessage(@PathVariable Long messageId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        messageService.recallMessage(messageId, userId);
        return Result.success();
    }

    @GetMapping("/can-send/{receiverId}")
    public Result<Map<String, Object>> canSendMessage(@PathVariable Long receiverId, HttpServletRequest request) {
        Long senderId = (Long) request.getAttribute("userId");
        boolean canSend = messageService.canSendMessage(senderId, receiverId);
        int remaining = messageService.getRemainingStrangerMessages(senderId, receiverId);
        return Result.success(Map.of("canSend", canSend, "remaining", remaining));
    }
}
