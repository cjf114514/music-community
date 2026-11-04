package com.music.repository;

import com.music.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SongRepository extends JpaRepository<Song, Long> {
    List<Song> findByNameContaining(String name);

    List<Song> findBySingerContaining(String singer);

    @Query(value = "SELECT * FROM song WHERE status = 1 ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Song> findRandomSongs(@Param("limit") int limit);

    @Query("SELECT s FROM Song s WHERE s.status = 1 AND (s.name LIKE %:keyword% OR s.singer LIKE %:keyword% OR s.album LIKE %:keyword%)")
    List<Song> searchAll(@Param("keyword") String keyword);

    List<Song> findByAlbumContaining(String album);

    org.springframework.data.domain.Page<Song> findByNameContaining(String name, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Song> findBySingerContaining(String singer, org.springframework.data.domain.Pageable pageable);

    long countBySinger(String singer);

    org.springframework.data.domain.Page<Song> findAllByOrderByPlayCountDesc(org.springframework.data.domain.Pageable pageable);

    List<Song> findByStatus(Integer status);

    List<Song> findBySingerContainingAndStatus(String singer, Integer status);

    List<Song> findByAlbumContainingAndStatus(String album, Integer status);

    List<Song> findByGenreAndStatus(String genre, Integer status);

    List<Song> findByGenreContainingAndStatus(String genre, Integer status);

    @Query("SELECT DISTINCT s.genre FROM Song s WHERE s.genre IS NOT NULL AND s.status = 1")
    List<String> findDistinctGenres();
}
