package com.music.repository;

import com.music.entity.PlayHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface PlayHistoryRepository extends JpaRepository<PlayHistory, Long> {
    @Query(value = "SELECT ph.* FROM play_history ph WHERE ph.user_id = :userId ORDER BY ph.play_time DESC LIMIT :limit", nativeQuery = true)
    List<PlayHistory> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    void deleteByUserIdAndSongId(Long userId, Long songId);

    void deleteByUserId(Long userId);

    @Query(value = "SELECT ph.song_id, COUNT(*) as play_count FROM play_history ph WHERE ph.play_time >= :startTime GROUP BY ph.song_id ORDER BY play_count DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopSongs(@Param("startTime") LocalDateTime startTime, @Param("limit") int limit);
    
    List<PlayHistory> findAllByOrderByPlayTimeDesc();
}
