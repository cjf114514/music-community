package com.music.repository;

import com.music.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserIdAndSongId(Long userId, Long songId);

    boolean existsByUserIdAndSongId(Long userId, Long songId);

    void deleteByUserIdAndSongId(Long userId, Long songId);

    @Query(value = "SELECT f.* FROM favorite f WHERE f.user_id = :userId ORDER BY f.create_time DESC", nativeQuery = true)
    List<Favorite> findByUserIdOrderByCreateTimeDesc(@Param("userId") Long userId);

    List<Favorite> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
