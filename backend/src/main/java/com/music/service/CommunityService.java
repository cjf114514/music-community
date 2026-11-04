package com.music.service;

import com.music.entity.CommunityPost;
import com.music.entity.PostComment;
import com.music.entity.PostCommentLike;
import com.music.entity.PostLike;
import com.music.entity.User;
import com.music.repository.CommunityPostRepository;
import com.music.repository.PostCommentLikeRepository;
import com.music.repository.PostCommentRepository;
import com.music.repository.PostLikeRepository;
import com.music.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityService {
    private final CommunityPostRepository communityPostRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentLikeRepository postCommentLikeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public CommunityPost createPost(Long userId, String title, String content, String coverUrl, String imageUrls, Long relatedSongId, Long relatedPlaylistId) {
        CommunityPost post = new CommunityPost();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        post.setCoverUrl(coverUrl);
        post.setImageUrls(imageUrls);
        post.setRelatedSongId(relatedSongId);
        post.setRelatedPlaylistId(relatedPlaylistId);
        return communityPostRepository.save(post);
    }

    @Transactional
    public CommunityPost updatePost(Long postId, Long userId, String title, String content, String coverUrl, String imageUrls, Long relatedSongId, Long relatedPlaylistId) {
        CommunityPost post = getPostById(postId);
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("无权编辑此帖子");
        }
        if (title != null) post.setTitle(title);
        if (content != null) post.setContent(content);
        post.setCoverUrl(coverUrl);
        post.setImageUrls(imageUrls);
        post.setRelatedSongId(relatedSongId);
        post.setRelatedPlaylistId(relatedPlaylistId);
        return communityPostRepository.save(post);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        CommunityPost post = getPostById(postId);
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此帖子");
        }
        postCommentRepository.deleteAll(postCommentRepository.findByPostIdAndParentIdIsNullOrderByCreateTimeDesc(postId));
        communityPostRepository.delete(post);
    }

    public List<CommunityPost> getAllPosts(int limit) {
        return communityPostRepository.findAllByOrderByCreateTimeDesc(PageRequest.of(0, limit));
    }

    public List<CommunityPost> getUserPosts(Long userId, int limit) {
        return communityPostRepository.findByUserIdOrderByCreateTimeDesc(userId, PageRequest.of(0, limit));
    }

    public CommunityPost getPostById(Long id) {
        return communityPostRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("帖子不存在"));
    }

    @Transactional
    public boolean toggleLikePost(Long postId, Long userId) {
        CommunityPost post = getPostById(postId);
        boolean exists = postLikeRepository.existsByUserIdAndPostId(userId, postId);
        if (exists) {
            postLikeRepository.deleteByUserIdAndPostId(userId, postId);
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            communityPostRepository.save(post);
            return false;
        } else {
            PostLike like = new PostLike();
            like.setUserId(userId);
            like.setPostId(postId);
            postLikeRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);
            communityPostRepository.save(post);
            User liker = userRepository.findById(userId).orElse(null);
            String nickname = liker != null ? (liker.getNickname() != null ? liker.getNickname() : liker.getUsername()) : "用户";
            if (!post.getUserId().equals(userId)) {
                notificationService.sendNotification(post.getUserId(), userId, "LIKE_POST", postId, nickname + " 赞了你的帖子");
            }
            return true;
        }
    }

    public boolean isPostLiked(Long postId, Long userId) {
        return postLikeRepository.existsByUserIdAndPostId(userId, postId);
    }

    @Transactional
    public PostComment createComment(Long postId, Long userId, String content, Long parentId) {
        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        PostComment saved = postCommentRepository.save(comment);

        CommunityPost post = getPostById(postId);
        post.setCommentCount(post.getCommentCount() + 1);
        communityPostRepository.save(post);

        User commenter = userRepository.findById(userId).orElse(null);
        String nickname = commenter != null ? (commenter.getNickname() != null ? commenter.getNickname() : commenter.getUsername()) : "用户";
        if (!post.getUserId().equals(userId)) {
            notificationService.sendNotification(post.getUserId(), userId, "COMMENT_POST", postId, nickname + " 评论了你的帖子");
        }

        return saved;
    }

    public List<Map<String, Object>> getPostComments(Long postId, String sortBy, Long userId) {
        List<PostComment> comments;
        if ("hot".equals(sortBy)) {
            comments = postCommentRepository.findByPostIdAndParentIdIsNullOrderByLikeCountDesc(postId);
        } else {
            comments = postCommentRepository.findByPostIdAndParentIdIsNullOrderByCreateTimeDesc(postId);
        }

        Set<Long> likedCommentIds = new HashSet<>();
        if (userId != null && !comments.isEmpty()) {
            List<Long> commentIds = comments.stream().map(PostComment::getId).collect(Collectors.toList());
            List<PostCommentLike> userLikes = postCommentLikeRepository.findByCommentIdInAndUserId(commentIds, userId);
            likedCommentIds = userLikes.stream().map(PostCommentLike::getCommentId).collect(Collectors.toSet());
        }

        Set<Long> finalLikedIds = likedCommentIds;
        return comments.stream().map(c -> {
            User user = userRepository.findById(c.getUserId()).orElse(null);
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("content", c.getContent());
            m.put("parentId", c.getParentId());
            m.put("likeCount", c.getLikeCount());
            m.put("liked", finalLikedIds.contains(c.getId()));
            m.put("createTime", c.getCreateTime());
            m.put("userNickname", user != null ? user.getNickname() : "未知用户");
            m.put("userAvatar", user != null ? user.getAvatar() : null);
            m.put("userId", c.getUserId());
            m.put("replyCount", postCommentRepository.countByParentId(c.getId()));
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public boolean toggleLikeComment(Long commentId, Long userId) {
        boolean exists = postCommentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
        if (exists) {
            postCommentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
            postCommentLikeRepository.decrementLikeCount(commentId);
            return false;
        } else {
            PostCommentLike like = new PostCommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            postCommentLikeRepository.save(like);
            postCommentLikeRepository.incrementLikeCount(commentId);
            return true;
        }
    }

    public List<PostComment> getCommentReplies(Long parentId) {
        return postCommentRepository.findByParentIdOrderByCreateTimeDesc(parentId);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此评论");
        }
        List<PostComment> replies = postCommentRepository.findByParentIdOrderByCreateTimeDesc(commentId);
        postCommentRepository.deleteAll(replies);
        postCommentRepository.delete(comment);
        CommunityPost post = communityPostRepository.findById(comment.getPostId()).orElse(null);
        if (post != null) {
            post.setCommentCount(Math.max(0, post.getCommentCount() - 1 - replies.size()));
            communityPostRepository.save(post);
        }
    }

    public List<CommunityPost> searchPosts(String keyword) {
        return communityPostRepository.findByTitleContainingOrContentContainingOrderByCreateTimeDesc(keyword, keyword);
    }
}
