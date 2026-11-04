package com.music.controller;

import com.music.annotation.OperationLogAnnotation;
import com.music.annotation.RequireAdmin;
import com.music.common.Result;
import com.music.entity.Comment;
import com.music.entity.User;
import com.music.repository.CommentRepository;
import com.music.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @GetMapping
    @RequireAdmin
    public Result<Map<String, Object>> getComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer status) {
        
        List<Comment> allComments = commentRepository.findAll();
        
        List<Comment> comments = allComments.stream()
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .toList();
        
        if (status != null) {
            comments = comments.stream()
                    .filter(c -> status.equals(c.getStatus()))
                    .toList();
        }
        
        int total = comments.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);
        
        List<Comment> pageComments = fromIndex < total ? comments.subList(fromIndex, toIndex) : List.of();
        
        List<Map<String, Object>> commentList = pageComments.stream().map(comment -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", comment.getId());
            map.put("content", comment.getContent());
            map.put("userId", comment.getUserId());
            map.put("songId", comment.getSongId());
            map.put("playlistId", comment.getPlaylistId());
            map.put("likeCount", comment.getLikeCount());
            map.put("status", comment.getStatus());
            map.put("createTime", comment.getCreateTime());
            
            User user = userRepository.findById(comment.getUserId()).orElse(null);
            if (user != null) {
                map.put("username", user.getUsername());
                map.put("nickname", user.getNickname());
                map.put("userAvatar", user.getAvatar());
            }
            
            return map;
        }).toList();
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("list", commentList);
        
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    @RequireAdmin
    @OperationLogAnnotation(module = "评论管理", action = "UPDATE", description = "修改评论状态", targetType = "comment")
    public Result<Void> updateCommentStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        
        Integer status = params.get("status");
        if (status != 0 && status != 1) {
            throw new RuntimeException("无效的状态");
        }
        
        comment.setStatus(status);
        commentRepository.save(comment);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @OperationLogAnnotation(module = "评论管理", action = "DELETE", description = "删除评论", targetType = "comment")
    public Result<Void> deleteComment(@PathVariable Long id) {
        if (!commentRepository.existsById(id)) {
            throw new RuntimeException("评论不存在");
        }
        commentRepository.deleteById(id);
        return Result.success();
    }
}
