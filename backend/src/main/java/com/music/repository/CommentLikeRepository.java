package com.music.repository;

import com.music.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Set;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    void deleteByCommentIdAndUserId(Long commentId, Long userId);
    List<CommentLike> findByUserId(Long userId);
    List<CommentLike> findByCommentIdInAndUserId(List<Long> commentIds, Long userId);
}
