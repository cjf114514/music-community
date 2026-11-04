package com.music.service;

import com.music.dto.CommentResponse;
import com.music.entity.Comment;
import com.music.entity.CommentLike;
import com.music.entity.Song;
import com.music.entity.User;
import com.music.entity.Playlist;
import com.music.repository.CommentLikeRepository;
import com.music.repository.CommentRepository;
import com.music.repository.SongRepository;
import com.music.repository.UserRepository;
import com.music.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PlaylistRepository playlistRepository;
    private final NotificationService notificationService;

    public Map<String, Object> getSongComments(Long songId, Long userId, String sortBy) {
        List<Comment> rootComments;
        //如果排序为“热门”，则按点赞数排序
        //parent_id 为 null 表示根评论，status 为 1 表示正常评论
        if ("hot".equals(sortBy)) { 
            rootComments = commentRepository.findBySongIdAndParentIdAndStatusOrderByLikeCountDesc(songId, null, 1);
        } else {
            // 否则按创建时间排序
            rootComments = commentRepository.findBySongIdAndParentIdAndStatusOrderByCreateTimeDesc(songId, null, 1);
        }
        // 获取这首歌所有回复评论
        List<Comment> allReplies = commentRepository.findBySongIdAndStatusOrderByCreateTimeDesc(songId, 1);
        Map<Long, List<Comment>> repliesMap = allReplies.stream() 
                .filter(c -> c.getParentId() != null)   //过滤器：只保留回复评论，不包含根评论
                .collect(Collectors.groupingBy(Comment::getParentId)); // 按 parentId 分组 → key=父评论ID，value=回复列表 如101: [评论A, 评论B], 102: [评论C]
        //set自动去重的集合，收集所有评论ID
        Set<Long> allCommentIds = new HashSet<>(); // 收集所有评论ID集合
        allCommentIds.addAll(rootComments.stream().map(Comment::getId).collect(Collectors.toSet())); // 收集根评论ID
        for (List<Comment> replies : repliesMap.values()) {
            allCommentIds.addAll(replies.stream().map(Comment::getId).collect(Collectors.toSet())); // 收集回复评论ID
        }
        // 创建一个空集合，用来存 用户点过赞的评论 ID
        Set<Long> likedCommentIds = new HashSet<>();
        if (userId != null && !allCommentIds.isEmpty()) {
            //new ArrayList<>(allCommentIds)所有评论id
            //userId 用户ID
            List<CommentLike> userLikes = commentLikeRepository.findByCommentIdInAndUserId(new ArrayList<>(allCommentIds), userId); 
            // 从点赞记录中提取点赞的评论ID 放进 Set集合，自动去重
            likedCommentIds = userLikes.stream().map(CommentLike::getCommentId).collect(Collectors.toSet());
        }

        Set<Long> finalLikedCommentIds = likedCommentIds; // 复制最终点赞的评论ID集合
        List<CommentResponse> commentList = rootComments.stream() //组装评论列表
                //c-根评论 repliesMap.getOrDefault-当前根评论的回复列表 finalLikedCommentIds-用户点赞的评论ID集合
                //最终变为{ 评论1 + 回复 + 点赞状态 },{ 评论2 + 回复 + 点赞状态 },
                .map(c -> buildCommentResponse(c, repliesMap.getOrDefault(c.getId(), new ArrayList<>()), finalLikedCommentIds))
                .collect(Collectors.toList());


        long totalCount = commentRepository.countBySongIdAndStatus(songId, 1); // 统计这首歌所有评论数

        Map<String, Object> result = new HashMap<>(); //封装成map返回给前端 前端收集到{"comments": [ ...评论列表... ],"totalCount": 128}
        result.put("comments", commentList); // 评论列表
        result.put("totalCount", totalCount); // 评论总数
        return result;
    }

    // 获取歌单所有评论
    public Map<String, Object> getPlaylistComments(Long playlistId, Long userId, String sortBy) {
        List<Comment> rootComments;
        if ("hot".equals(sortBy)) {
            rootComments = commentRepository.findByPlaylistIdAndParentIdAndStatusOrderByLikeCountDesc(playlistId, null, 1);
        } else {
            rootComments = commentRepository.findByPlaylistIdAndParentIdAndStatusOrderByCreateTimeDesc(playlistId, null, 1);
        }

        List<Comment> allReplies = commentRepository.findByPlaylistIdAndStatusOrderByCreateTimeDesc(playlistId, 1);
        Map<Long, List<Comment>> repliesMap = allReplies.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentId));

        Set<Long> allCommentIds = new HashSet<>();
        allCommentIds.addAll(rootComments.stream().map(Comment::getId).collect(Collectors.toSet()));
        for (List<Comment> replies : repliesMap.values()) {
            allCommentIds.addAll(replies.stream().map(Comment::getId).collect(Collectors.toSet()));
        }

        Set<Long> likedCommentIds = new HashSet<>();
        if (userId != null && !allCommentIds.isEmpty()) {
            List<CommentLike> userLikes = commentLikeRepository.findByCommentIdInAndUserId(new ArrayList<>(allCommentIds), userId);
            likedCommentIds = userLikes.stream().map(CommentLike::getCommentId).collect(Collectors.toSet());
        }

        Set<Long> finalLikedCommentIds = likedCommentIds;
        List<CommentResponse> commentList = rootComments.stream()
                .map(c -> buildCommentResponse(c, repliesMap.getOrDefault(c.getId(), new ArrayList<>()), finalLikedCommentIds))
                .collect(Collectors.toList());

        long totalCount = commentRepository.countByPlaylistIdAndStatus(playlistId, 1);

        Map<String, Object> result = new HashMap<>();
        result.put("comments", commentList);
        result.put("totalCount", totalCount);
        return result;
    }

    private CommentResponse buildCommentResponse(Comment comment, List<Comment> replies, Set<Long> likedCommentIds) {
        //三个参数：comment-根评论 replies-当前根评论的回复列表 likedCommentIds-用户点赞的评论ID集合
        User user = userRepository.findById(comment.getUserId()).orElse(null); //查询评论发布者的用户信息，若不存在则返回null值
        //. 开始构建 CommentResponse（给前端用的评论结构）
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setUserId(comment.getUserId());
        response.setUsername(user != null ? user.getUsername() : "未知用户"); // 评论发布者的用户名，不存在则为"未知用户"
        response.setNickname(user != null ? user.getNickname() : "未知用户"); // 评论发布者的昵称，不存在则为"未知用户"
        response.setAvatar(user != null ? user.getAvatar() : null); // 评论发布者的头像，不存在则为null
        response.setSongId(comment.getSongId());
        response.setPlaylistId(comment.getPlaylistId());
        response.setParentId(comment.getParentId());
        response.setTimestamp(comment.getTimestamp());
        response.setLikeCount(comment.getLikeCount());
        response.setCreateTime(comment.getCreateTime());
        response.setLiked(likedCommentIds.contains(comment.getId())); // 是否点赞 likedCommentIds 是用户点过赞的集合

        List<Comment> sortedReplies = replies.stream()
                .sorted(Comparator.comparing(Comment::getCreateTime)) //排序规则：按创建时间降序 保证先发表的在上面，后发表的在下面
                .collect(Collectors.toList()); // 收集为列表

        response.setReplies(sortedReplies.stream()
                .map(r -> buildReplyResponse(r, likedCommentIds)) //把每一条回复 r，交给 buildReplyResponse 处理，返回回复的 CommentResponse 结构 变成前端能看懂的回复格式（带昵称、头像、点赞状态）
                .collect(Collectors.toList())); //把处理好的所有回复，重新变成一个列表
        return response;
    }

    private CommentResponse buildReplyResponse(Comment reply, Set<Long> likedCommentIds) {
        User replyUser = userRepository.findById(reply.getUserId()).orElse(null);
        User replyToUser = null;

        String replyContent = reply.getContent();
        String displayContent = replyContent;

        if (replyContent.startsWith("@")) {
            int colonIndex = replyContent.indexOf("：");
            int spaceIndex = replyContent.indexOf(" ");
            int endIndex = -1;
            if (colonIndex > 0 && (spaceIndex < 0 || colonIndex < spaceIndex)) {
                endIndex = colonIndex;
            } else if (spaceIndex > 0) {
                endIndex = spaceIndex;
            }

            if (endIndex > 1) {
                String mentionedName = replyContent.substring(1, endIndex);
                List<User> allUsers = userRepository.findAll();
                replyToUser = allUsers.stream()
                        .filter(u -> mentionedName.equals(u.getNickname()) || mentionedName.equals(u.getUsername()))
                        .findFirst()
                        .orElse(null);

                if (replyToUser != null) {
                    displayContent = replyContent.substring(endIndex + 1).trim();
                }
            }
        }

        CommentResponse replyResponse = new CommentResponse();
        replyResponse.setId(reply.getId());
        replyResponse.setContent(displayContent);
        replyResponse.setOriginalContent(replyContent);
        replyResponse.setUserId(reply.getUserId());
        replyResponse.setUsername(replyUser != null ? replyUser.getUsername() : "未知用户");
        replyResponse.setNickname(replyUser != null ? replyUser.getNickname() : "未知用户");
        replyResponse.setAvatar(replyUser != null ? replyUser.getAvatar() : null);
        replyResponse.setSongId(reply.getSongId());
        replyResponse.setPlaylistId(reply.getPlaylistId());
        replyResponse.setParentId(reply.getParentId());
        replyResponse.setTimestamp(reply.getTimestamp());
        replyResponse.setLikeCount(reply.getLikeCount());
        replyResponse.setCreateTime(reply.getCreateTime());
        replyResponse.setLiked(likedCommentIds.contains(reply.getId()));
        replyResponse.setReplies(new ArrayList<>());

        if (replyToUser != null) {
            replyResponse.setReplyToUserId(replyToUser.getId());
            replyResponse.setReplyToNickname(replyToUser.getNickname());
        }

        return replyResponse;
    }

    @Transactional
    public CommentResponse addComment(Long songId, Long userId, String content, Long parentId) {
        return addComment(songId, userId, content, parentId, null);
    }

    @Transactional
    public CommentResponse addComment(Long songId, Long userId, String content, Long parentId, Integer timestamp) {
        Comment comment = new Comment();
        comment.setSongId(songId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setTimestamp(timestamp);
        comment.setTargetType("song");
        commentRepository.save(comment);

        Song song = songRepository.findById(songId).orElse(null);
        if (song != null) {
            song.setPlayCount(song.getPlayCount() != null ? song.getPlayCount() + 1 : 1); // 增加歌曲播放量
            songRepository.save(song); // 保存歌曲信息
        }

        User commenter = userRepository.findById(userId).orElse(null);
        String nickname = commenter != null ? (commenter.getNickname() != null ? commenter.getNickname() : commenter.getUsername()) : "用户";
        if (parentId != null) { // 如果有父评论ID，则发送回复通知
            Comment parent = commentRepository.findById(parentId).orElse(null);
            if (parent != null && !parent.getUserId().equals(userId)) { // 如果父评论存在且不是当前用户，则发送回复通知 
                notificationService.sendNotification(parent.getUserId(), userId, "REPLY", comment.getId(), nickname + " 回复了你的评论");
            }
        }

        //返回前端可直接显示的评论结构
        Set<Long> likedIds = new HashSet<>();
        likedIds.add(comment.getId()); 
        return buildCommentResponse(comment, new ArrayList<>(), likedIds);  
    }

    @Transactional
    public CommentResponse addPlaylistComment(Long playlistId, Long userId, String content, Long parentId) {
        Comment comment = new Comment();
        comment.setPlaylistId(playlistId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setTargetType("playlist");
        commentRepository.save(comment);

        User commenter = userRepository.findById(userId).orElse(null);
        String nickname = commenter != null ? (commenter.getNickname() != null ? commenter.getNickname() : commenter.getUsername()) : "用户"; // 获取评论昵称或用户名
        if (parentId != null) { // 如果有父评论ID，则发送回复通知
            Comment parent = commentRepository.findById(parentId).orElse(null); // 查找父评论
            if (parent != null && !parent.getUserId().equals(userId)) { // 如果父评论存在且不是当前用户，则发送回复通知 
                notificationService.sendNotification(parent.getUserId(), userId, "REPLY", comment.getId(), nickname + " 回复了你的评论");
            }
        }

        Set<Long> likedIds = new HashSet<>(); 
        likedIds.add(comment.getId()); // 添加评论ID到点赞列表
        return buildCommentResponse(comment, new ArrayList<>(), likedIds); 
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId).orElse(null); // 查找评论
        if (comment != null && comment.getUserId().equals(userId)) { // 如果评论存在且用户ID匹配
            comment.setStatus(0); // 设置评论状态为已删除
            commentRepository.save(comment); // 保存评论信息
            //当为歌曲评论时，把它下面的【所有子回复】也一起标记为删除！
            if ("song".equals(comment.getTargetType()) && comment.getSongId() != null) {
                List<Comment> replies = commentRepository.findBySongIdAndParentIdAndStatus(comment.getSongId(), commentId, 1);
                for (Comment reply : replies) {
                    reply.setStatus(0);
                    commentRepository.save(reply);
                }
            //当为歌单评论时，把它下面的【所有子回复】也一起标记为删除！
            } else if ("playlist".equals(comment.getTargetType()) && comment.getPlaylistId() != null) {
                List<Comment> replies = commentRepository.findByPlaylistIdAndParentIdAndStatus(comment.getPlaylistId(), commentId, 1);
                for (Comment reply : replies) {
                    reply.setStatus(0);
                    commentRepository.save(reply);
                }
            }

            commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
        }
    }

    @Transactional
    // 点赞/取消点赞评论
    public boolean toggleLike(Long commentId, Long userId) {
        //查 comment_like 表，有没有这条 评论 ID + 用户 ID 的记录
        boolean exists = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
        if (exists) {
            //如果存在，说明用户点赞了，现在要取消点赞
            commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
            //取消点赞后，评论点赞数减1
            commentRepository.decrementLikeCount(commentId);
            return false;
        } else {
            //如果不存在，说明用户没有点赞，现在要点赞
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            commentLikeRepository.save(like);
            //点赞后，评论点赞数加1
            commentRepository.incrementLikeCount(commentId);
            //发送点赞通知给作者
            Comment comment = commentRepository.findById(commentId).orElse(null);
            if (comment != null && !comment.getUserId().equals(userId)) {
                User liker = userRepository.findById(userId).orElse(null);
                String nickname = liker != null ? (liker.getNickname() != null ? liker.getNickname() : liker.getUsername()) : "用户";
                notificationService.sendNotification(comment.getUserId(), userId, "LIKE_COMMENT", commentId, nickname + " 赞了你的评论");
            }
            return true;
        }
    }

    public boolean isLiked(Long commentId, Long userId) {
        return commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
    }

    //获取歌曲的弹幕评论（已停用）
    public List<CommentResponse> getSongDanmaku(Long songId, Long userId) {
        List<Comment> danmakuComments = commentRepository.findDanmakuBySongId(songId);

        Set<Long> commentIds = danmakuComments.stream()
                .map(Comment::getId)
                .collect(Collectors.toSet());

        Set<Long> likedIds = new HashSet<>();
        if (userId != null && !commentIds.isEmpty()) {
            List<CommentLike> userLikes = commentLikeRepository.findByCommentIdInAndUserId(new ArrayList<>(commentIds), userId);
            likedIds = userLikes.stream().map(CommentLike::getCommentId).collect(Collectors.toSet());
        }

        Set<Long> finalLikedIds = likedIds;
        return danmakuComments.stream()
                .filter(c -> c.getParentId() == null)
                .map(c -> {
                    User user = userRepository.findById(c.getUserId()).orElse(null);
                    CommentResponse resp = new CommentResponse();
                    resp.setId(c.getId());
                    resp.setContent(c.getContent());
                    resp.setUserId(c.getUserId());
                    resp.setNickname(user != null ? user.getNickname() : "未知用户");
                    resp.setAvatar(user != null ? user.getAvatar() : null);
                    resp.setSongId(c.getSongId());
                    resp.setTimestamp(c.getTimestamp());
                    resp.setLikeCount(c.getLikeCount());
                    resp.setCreateTime(c.getCreateTime());
                    resp.setLiked(finalLikedIds.contains(c.getId()));
                    resp.setReplies(new ArrayList<>());
                    return resp;
                })
                .collect(Collectors.toList());
    }
}
