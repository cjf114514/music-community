package com.music.repository;

import com.music.entity.SongShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SongShareRepository extends JpaRepository<SongShare, Long> {
    Optional<SongShare> findByShareToken(String shareToken);
    Optional<SongShare> findBySongId(Long songId);
}
