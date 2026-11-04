package com.music.repository;

import com.music.entity.PostCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PostCommentLikeRepository extends JpaRepository<PostCommentLike, Long> {
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    void deleteByCommentIdAndUserId(Long commentId, Long userId);
    List<PostCommentLike> findByCommentIdInAndUserId(List<Long> commentIds, Long userId);

    @Modifying // UPDATE / DELETE 语句需要@Modifying 注解 同时使用时也必须加@Transactional 注解 否则会报错
    @Query("UPDATE PostComment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE PostComment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :id AND c.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);
}
