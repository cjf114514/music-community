package com.music.repository;

import com.music.entity.PlaylistShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlaylistShareRepository extends JpaRepository<PlaylistShare, Long> {
    Optional<PlaylistShare> findByShareToken(String shareToken);
    Optional<PlaylistShare> findByPlaylistId(Long playlistId);
}
