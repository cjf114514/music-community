package com.music.controller;

import com.music.common.Result;
import com.music.dto.CommentResponse;
import com.music.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/song/{songId}")
    public Result<Map<String, Object>> getSongComments(@PathVariable Long songId,
                                                        @RequestParam(required = false, defaultValue = "time") String sortBy,
                                                        HttpServletRequest request) {
        Long userId = null;
        try {
            userId = (Long) request.getAttribute("userId");
        } catch (Exception e) {}
        return Result.success(commentService.getSongComments(songId, userId, sortBy));
    }

    @GetMapping("/song/{songId}/danmaku")
    public Result<List<CommentResponse>> getSongDanmaku(@PathVariable Long songId,
                                                         HttpServletRequest request) {
        Long userId = null;
        try {
            userId = (Long) request.getAttribute("userId");
        } catch (Exception e) {}
        return Result.success(commentService.getSongDanmaku(songId, userId));
    }

    @PostMapping("/song/{songId}")
    public Result<CommentResponse> addComment(@PathVariable Long songId,
                                               @RequestBody Map<String, Object> body,
                                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String content = (String) body.get("content");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        Integer timestamp = body.get("timestamp") != null ? Integer.valueOf(body.get("timestamp").toString()) : null;
        return Result.success(commentService.addComment(songId, userId, content, parentId, timestamp));
    }

    @DeleteMapping("/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        commentService.deleteComment(commentId, userId);
        return Result.success();
    }

    @PostMapping("/{commentId}/like")
    public Result<Boolean> toggleLike(@PathVariable Long commentId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean liked = commentService.toggleLike(commentId, userId);
        return Result.success(liked);
    }

    @GetMapping("/playlist/{playlistId}")
    public Result<Map<String, Object>> getPlaylistComments(@PathVariable Long playlistId,
                                                            @RequestParam(required = false, defaultValue = "time") String sortBy,
                                                            HttpServletRequest request) {
        Long userId = null;
        try {
            userId = (Long) request.getAttribute("userId");
        } catch (Exception e) {}
        return Result.success(commentService.getPlaylistComments(playlistId, userId, sortBy));
    }

    @PostMapping("/playlist/{playlistId}")
    public Result<CommentResponse> addPlaylistComment(@PathVariable Long playlistId,
                                                       @RequestBody Map<String, Object> body,
                                                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String content = (String) body.get("content");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        return Result.success(commentService.addPlaylistComment(playlistId, userId, content, parentId));
    }
}
