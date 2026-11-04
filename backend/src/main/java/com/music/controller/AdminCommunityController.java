package com.music.controller;

import com.music.annotation.OperationLogAnnotation;
import com.music.annotation.RequireAdmin;
import com.music.common.Result;
import com.music.entity.CommunityPost;
import com.music.entity.PostComment;
import com.music.entity.User;
import com.music.repository.CommunityPostRepository;
import com.music.repository.PostCommentRepository;
import com.music.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/community")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final CommunityPostRepository communityPostRepository;
    private final PostCommentRepository postCommentRepository;
    private final UserRepository userRepository;

    @GetMapping("/posts")
    @RequireAdmin
    public Result<Map<String, Object>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<CommunityPost> postPage;

        if (keyword != null && !keyword.isEmpty()) {
            List<CommunityPost> all = communityPostRepository.findAll().stream()
                    .filter(p -> p.getTitle().contains(keyword) || p.getContent().contains(keyword))
                    .toList();
            int total = all.size();
            int from = page * size;
            int to = Math.min(from + size, total);
            List<CommunityPost> list = from < total ? all.subList(from, to) : List.of();

            List<Map<String, Object>> postList = list.stream().map(this::toPostMap).toList();
            Map<String, Object> result = new HashMap<>();
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("list", postList);
            return Result.success(result);
        }

        postPage = communityPostRepository.findAll(pageable);
        List<Map<String, Object>> postList = postPage.getContent().stream().map(this::toPostMap).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("total", postPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("list", postList);
        return Result.success(result);
    }

    @DeleteMapping("/posts/{id}")
    @RequireAdmin
    @OperationLogAnnotation(module = "社区管理", action = "DELETE", description = "删除帖子", targetType = "post")
    public Result<Void> deletePost(@PathVariable Long id) {
        if (!communityPostRepository.existsById(id)) {
            throw new RuntimeException("帖子不存在");
        }
        List<PostComment> comments = postCommentRepository.findByPostIdAndParentIdIsNullOrderByCreateTimeDesc(id);
        postCommentRepository.deleteAll(comments);
        communityPostRepository.deleteById(id);
        return Result.success();
    }

    @GetMapping("/posts/{postId}/comments")
    @RequireAdmin
    public Result<Map<String, Object>> getPostComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        List<PostComment> allComments = postCommentRepository.findByPostIdOrderByCreateTimeAsc(postId);
        int total = allComments.size();

        Map<Long, List<PostComment>> childrenMap = new HashMap<>();
        List<PostComment> rootComments = new ArrayList<>();
        for (PostComment c : allComments) {
            if (c.getParentId() == null) {
                rootComments.add(c);
            } else {
                childrenMap.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
            }
        }

        int from = page * size;
        int to = Math.min(from + size, rootComments.size());
        List<PostComment> pagedRoots = from < rootComments.size() ? rootComments.subList(from, to) : List.of();

        List<Map<String, Object>> commentList = pagedRoots.stream().map(c -> toCommentMap(c, childrenMap)).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("list", commentList);
        return Result.success(result);
    }

    private Map<String, Object> toCommentMap(PostComment c, Map<Long, List<PostComment>> childrenMap) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("postId", c.getPostId());
        map.put("userId", c.getUserId());
        map.put("content", c.getContent());
        map.put("parentId", c.getParentId());
        map.put("likeCount", c.getLikeCount());
        map.put("createTime", c.getCreateTime());

        User user = userRepository.findById(c.getUserId()).orElse(null);
        if (user != null) {
            map.put("username", user.getUsername());
            map.put("nickname", user.getNickname());
            map.put("userAvatar", user.getAvatar());
        }

        List<PostComment> replies = childrenMap.getOrDefault(c.getId(), List.of());
        if (!replies.isEmpty()) {
            map.put("replies", replies.stream().map(r -> toCommentMap(r, childrenMap)).toList());
        }
        return map;
    }

    @DeleteMapping("/comments/{id}")
    @RequireAdmin
    @OperationLogAnnotation(module = "社区管理", action = "DELETE", description = "删除社区评论", targetType = "comment")
    public Result<Void> deleteComment(@PathVariable Long id) {
        PostComment comment = postCommentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        Long postId = comment.getPostId();
        postCommentRepository.deleteById(id);
        CommunityPost post = communityPostRepository.findById(postId).orElse(null);
        if (post != null) {
            post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
            communityPostRepository.save(post);
        }
        return Result.success();
    }

    private Map<String, Object> toPostMap(CommunityPost post) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", post.getId());
        map.put("userId", post.getUserId());
        map.put("title", post.getTitle());
        map.put("content", post.getContent());
        map.put("coverUrl", post.getCoverUrl());
        map.put("likeCount", post.getLikeCount());
        map.put("commentCount", postCommentRepository.countByPostId(post.getId()));
        map.put("createTime", post.getCreateTime());
        map.put("updateTime", post.getUpdateTime());

        User user = userRepository.findById(post.getUserId()).orElse(null);
        if (user != null) {
            map.put("username", user.getUsername());
            map.put("nickname", user.getNickname());
            map.put("userAvatar", user.getAvatar());
        }
        return map;
    }
}
