package com.music.repository;

import com.music.entity.CommunityPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {
    List<CommunityPost> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);
    List<CommunityPost> findAllByOrderByCreateTimeDesc(Pageable pageable);
    List<CommunityPost> findByTitleContainingOrContentContainingOrderByCreateTimeDesc(String title, String content);
}
