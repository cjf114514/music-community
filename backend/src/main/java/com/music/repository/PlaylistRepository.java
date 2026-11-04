package com.music.repository;

import com.music.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByUserIdAndStatus(Long userId, Integer status);

    List<Playlist> findByIsPublicAndStatus(Integer isPublic, Integer status);

    List<Playlist> findByCategoryAndStatus(String category, Integer status);

    @Query("SELECT DISTINCT p.category FROM Playlist p WHERE p.category IS NOT NULL AND p.status = 1")
    List<String> findDistinctCategories();

    @Query("SELECT p FROM Playlist p WHERE p.status = 1 AND (:keyword IS NULL OR p.name LIKE %:keyword% OR p.description LIKE %:keyword%)")
    List<Playlist> searchPlaylists(@Param("keyword") String keyword);
}
