package com.music.repository;

import com.music.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
    List<UserFollow> findByFollowerId(Long followerId);
    List<UserFollow> findByFollowingId(Long followingId);

    @Query("SELECT COUNT(f) FROM UserFollow f WHERE f.followingId = :userId")
    long countFollowersByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM UserFollow f WHERE f.followerId = :userId")
    long countFollowingByUserId(@Param("userId") Long userId);

    @Query("SELECT f.followingId FROM UserFollow f WHERE f.followerId = :userId")
    List<Long> findFollowingIdsByFollowerId(@Param("userId") Long userId);
}
