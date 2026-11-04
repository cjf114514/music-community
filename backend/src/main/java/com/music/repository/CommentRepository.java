package com.music.repository;

import com.music.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findBySongIdAndParentIdAndStatusOrderByCreateTimeDesc(Long songId, Long parentId, Integer status);

    List<Comment> findBySongIdAndStatusOrderByCreateTimeDesc(Long songId, Integer status);

    List<Comment> findBySongIdAndParentIdAndStatusOrderByLikeCountDesc(Long songId, Long parentId, Integer status);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :id AND c.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);

    long countBySongIdAndStatus(Long songId, Integer status);

    List<Comment> findBySongIdAndParentIdAndStatus(Long songId, Long parentId, Integer status);

    List<Comment> findByPlaylistIdAndParentIdAndStatusOrderByCreateTimeDesc(Long playlistId, Long parentId, Integer status);

    List<Comment> findByPlaylistIdAndStatusOrderByCreateTimeDesc(Long playlistId, Integer status);

    List<Comment> findByPlaylistIdAndParentIdAndStatusOrderByLikeCountDesc(Long playlistId, Long parentId, Integer status);

    long countByPlaylistIdAndStatus(Long playlistId, Integer status);

    List<Comment> findByPlaylistIdAndParentIdAndStatus(Long playlistId, Long parentId, Integer status);

    @Query("SELECT c FROM Comment c WHERE c.songId = :songId AND c.timestamp IS NOT NULL AND c.status = 1 ORDER BY c.timestamp ASC")
    List<Comment> findDanmakuBySongId(@Param("songId") Long songId);
}
