package com.music.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse { 
    private Long id; // 评论ID
    private String content; // 评论内容
    private String originalContent; // 原始评论内容，用于显示在评论列表中
    private Long userId; // 评论发布者的用户ID
    private String username;  // 评论发布者的用户名
    private String nickname; // 评论发布者的昵称
    private String avatar; // 评论发布者的头像，不存在则为null
    private Long songId; // 评论关联的歌曲ID，不存在则为null
    private Long playlistId; // 评论关联的播放列表ID，不存在则为null
    private Long parentId; // 评论的父评论ID，不存在则为null
    private Long replyToUserId; // 回复的用户ID，不存在则为null
    private String replyToNickname; // 回复的用户昵称，不存在则为null
    private Integer timestamp; // 评论时间戳，用于排序
    private Integer likeCount; // 评论点赞数
    private Boolean liked; // 是否点赞
    private LocalDateTime createTime;
    private List<CommentResponse> replies; // 评论回复列表
}
