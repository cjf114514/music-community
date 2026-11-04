package com.music.controller;

import com.music.common.Result;
import com.music.entity.CommunityPost;
import com.music.entity.PostComment;
import com.music.entity.Song;
import com.music.entity.Playlist;
import com.music.entity.User;
import com.music.service.CommunityService;
import com.music.repository.UserRepository;
import com.music.repository.SongRepository;
import com.music.repository.PlaylistRepository;
import com.music.repository.PostCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController // 控制器，处理HTTP请求，返回JSON格式的数据
@RequestMapping("/api/community") // 映射路径，/api/community 开头的请求都会被这个方法处理  
@RequiredArgsConstructor // 构造函数注入依赖 ，在创建对象时自动注入依赖
public class CommunityController {
    private final CommunityService communityService;
    private final UserRepository userRepository;
    private final SongRepository songRepository;
    private final PlaylistRepository playlistRepository;
    private final PostCommentRepository postCommentRepository;

    //把一个社区帖子 CommunityPost 对象，转换成一个 Map（键值对），
    //最终返回给前端，前端就能直接渲染页面了。
    private Map<String, Object> toPostMap(CommunityPost p) {
        User user = userRepository.findById(p.getUserId()).orElse(null);
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId()); // 帖子ID
        m.put("userId", p.getUserId()); // 帖子创建者ID
        m.put("title", p.getTitle()); // 帖子标题
        m.put("content", p.getContent()); // 帖子内容
        m.put("coverUrl", p.getCoverUrl()); // 帖子封面URL
        m.put("imageUrls", p.getImageUrls()); // 帖子图片URL列表
        m.put("relatedSongId", p.getRelatedSongId()); // 帖子关联的歌曲ID
        m.put("relatedPlaylistId", p.getRelatedPlaylistId()); // 帖子关联的播放列表ID
        m.put("likeCount", p.getLikeCount()); // 帖子点赞数
        m.put("commentCount", postCommentRepository.countByPostId(p.getId())); // 帖子评论数
        m.put("createTime", p.getCreateTime()); // 帖子创建时间
        m.put("userNickname", user != null ? user.getNickname() : "未知用户"); // 帖子创建者昵称
        m.put("userAvatar", user != null ? user.getAvatar() : null); // 帖子创建者头像
        // 如果帖子关联了歌曲，就添加歌曲信息
        if (p.getRelatedSongId() != null) {
            Song song = songRepository.findById(p.getRelatedSongId()).orElse(null);
            if (song != null) {
                Map<String, Object> sm = new HashMap<>();
                sm.put("id", song.getId()); // 歌曲ID
                sm.put("name", song.getName()); // 歌曲名称
                sm.put("singer", song.getSinger()); // 歌曲歌手
                sm.put("coverUrl", song.getCoverUrl()); // 歌曲封面URL
                sm.put("album", song.getAlbum()); // 歌曲专辑
                sm.put("url", song.getUrl()); // 歌曲URL
                sm.put("duration", song.getDuration()); // 歌曲时长
                m.put("relatedSong", sm); // 关联的歌曲信息
            }
        }
        // 如果帖子关联了歌单，就添加播放列表信息
        if (p.getRelatedPlaylistId() != null) {
            Playlist pl = playlistRepository.findById(p.getRelatedPlaylistId()).orElse(null);
            if (pl != null) {
                Map<String, Object> pm = new HashMap<>();
                pm.put("id", pl.getId());
                pm.put("name", pl.getName());
                pm.put("coverUrl", pl.getCoverUrl());
                pm.put("description", pl.getDescription());
                m.put("relatedPlaylist", pm);
            }
        }
        return m;
    }

    @PostMapping("/post")
    public Result<Map<String, Object>> createPost(
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        String coverUrl = (String) params.get("coverUrl");
        String imageUrls = (String) params.get("imageUrls");
        Long relatedSongId = params.get("relatedSongId") != null ? ((Number) params.get("relatedSongId")).longValue() : null;
        Long relatedPlaylistId = params.get("relatedPlaylistId") != null ? ((Number) params.get("relatedPlaylistId")).longValue() : null;

        CommunityPost post = communityService.createPost(userId, title, content, coverUrl, imageUrls, relatedSongId, relatedPlaylistId);
        return Result.success(toPostMap(post));
    }

    @PutMapping("/posts/{postId}")
    public Result<Map<String, Object>> updatePost(
            @PathVariable Long postId,
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        String coverUrl = (String) params.get("coverUrl");
        String imageUrls = (String) params.get("imageUrls");
        Long relatedSongId = params.get("relatedSongId") != null ? ((Number) params.get("relatedSongId")).longValue() : null;
        Long relatedPlaylistId = params.get("relatedPlaylistId") != null ? ((Number) params.get("relatedPlaylistId")).longValue() : null;

        CommunityPost post = communityService.updatePost(postId, userId, title, content, coverUrl, imageUrls, relatedSongId, relatedPlaylistId);
        return Result.success(toPostMap(post));
    }

    // 删除帖子
    @DeleteMapping("/posts/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        communityService.deletePost(postId, userId);
        return Result.success(null);
    }

    // 获取所有帖子
    @GetMapping("/posts")
    public Result<List<Map<String, Object>>> getPosts(@RequestParam(defaultValue = "20") int limit) {
        List<CommunityPost> posts = communityService.getAllPosts(limit);
        return Result.success(posts.stream().map(this::toPostMap).collect(Collectors.toList()));
    }

    // 获取指定用户发布的帖子
    @GetMapping("/posts/user/{userId}")
    public Result<List<Map<String, Object>>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        List<CommunityPost> posts = communityService.getUserPosts(userId, limit);
        return Result.success(posts.stream().map(this::toPostMap).collect(Collectors.toList()));
    }

    // 获取指定帖子详情
    @GetMapping("/posts/{postId}")
    public Result<Map<String, Object>> getPost(@PathVariable Long postId) {
        CommunityPost post = communityService.getPostById(postId);
        return Result.success(toPostMap(post));
    }

    // 点赞帖子
    @PostMapping("/posts/{postId}/like")
    public Result<Map<String, Object>> likePost(@PathVariable Long postId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean liked = communityService.toggleLikePost(postId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        return Result.success(result);
    }

    // 获取指定帖子的点赞状态
    @GetMapping("/posts/{postId}/like-status")
    public Result<Map<String, Object>> getLikeStatus(@PathVariable Long postId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean liked = communityService.isPostLiked(postId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        return Result.success(result);
    }

    // 创建评论
    @PostMapping("/posts/{postId}/comment")
    public Result<Map<String, Object>> createComment(
            @PathVariable Long postId,
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String content = (String) params.get("content");
        Long parentId = params.get("parentId") != null ? ((Number) params.get("parentId")).longValue() : null;

        PostComment comment = communityService.createComment(postId, userId, content, parentId);
        User user = userRepository.findById(userId).orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("id", comment.getId());
        result.put("content", comment.getContent());
        result.put("parentId", comment.getParentId());
        result.put("likeCount", comment.getLikeCount());
        result.put("createTime", comment.getCreateTime());
        result.put("userNickname", user != null ? user.getNickname() : "未知用户");
        result.put("userAvatar", user != null ? user.getAvatar() : null);
        result.put("userId", userId);
        return Result.success(result);
    }

    // 获取指定帖子的评论
    @GetMapping("/posts/{postId}/comments")
    public Result<List<Map<String, Object>>> getPostComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "time") String sortBy,
            HttpServletRequest request) {
        Long userId = null;
        try {
            userId = (Long) request.getAttribute("userId");
        } catch (Exception e) {}
        List<Map<String, Object>> comments = communityService.getPostComments(postId, sortBy, userId);
        return Result.success(comments);
    }

    // 点赞评论
    @PostMapping("/comments/{commentId}/like")
    public Result<Map<String, Object>> likeComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean liked = communityService.toggleLikeComment(commentId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        return Result.success(result);
    }

    // 获取指定评论的回复
    @GetMapping("/posts/{postId}/comments/{parentId}/replies")
    public Result<List<Map<String, Object>>> getCommentReplies(
            @PathVariable Long postId,
            @PathVariable Long parentId) {
        List<PostComment> replies = communityService.getCommentReplies(parentId);
        return Result.success(replies.stream().map(r -> {
            User user = userRepository.findById(r.getUserId()).orElse(null);
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("content", r.getContent());
            m.put("parentId", r.getParentId());
            m.put("likeCount", r.getLikeCount());
            m.put("createTime", r.getCreateTime());
            m.put("userNickname", user != null ? user.getNickname() : "未知用户");
            m.put("userAvatar", user != null ? user.getAvatar() : null);
            m.put("userId", r.getUserId());
            return m;
        }).collect(Collectors.toList()));
    }

    // 搜索帖子
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> searchPosts(@RequestParam String keyword) {
        List<CommunityPost> posts = communityService.searchPosts(keyword);
        return Result.success(posts.stream().map(this::toPostMap).collect(Collectors.toList()));
    }

    // 删除评论
    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        communityService.deleteComment(commentId, userId);
        return Result.success();
    }
}
