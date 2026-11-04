package com.music.repository;

import com.music.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}
