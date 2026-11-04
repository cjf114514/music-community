package com.music.repository;

import com.music.entity.PlaylistSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, Long> {
    List<PlaylistSong> findByPlaylistIdOrderBySortOrder(Long playlistId);

    Optional<PlaylistSong> findByPlaylistIdAndSongId(Long playlistId, Long songId);

    @Modifying
    @Query("DELETE FROM PlaylistSong ps WHERE ps.playlistId = :playlistId AND ps.songId = :songId")
    void deleteByPlaylistIdAndSongId(@Param("playlistId") Long playlistId, @Param("songId") Long songId);

    long countByPlaylistId(Long playlistId);

    @Modifying
    @Query("UPDATE PlaylistSong ps SET ps.sortOrder = :sortOrder WHERE ps.playlistId = :playlistId AND ps.songId = :songId")
    void updateSortOrder(@Param("playlistId") Long playlistId, @Param("songId") Long songId, @Param("sortOrder") Integer sortOrder);
}
