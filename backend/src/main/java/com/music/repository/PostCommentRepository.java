package com.music.repository;

import com.music.entity.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    List<PostComment> findByPostIdAndParentIdIsNullOrderByCreateTimeDesc(Long postId);
    List<PostComment> findByPostIdAndParentIdIsNullOrderByLikeCountDesc(Long postId);
    List<PostComment> findByParentIdOrderByCreateTimeDesc(Long parentId);
    List<PostComment> findByPostIdOrderByCreateTimeAsc(Long postId);
    long countByParentId(Long parentId);
    long countByPostId(Long postId);
}
