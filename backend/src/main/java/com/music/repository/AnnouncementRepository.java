package com.music.repository;

import com.music.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    Page<Announcement> findByStatusOrderByIsTopDescCreateTimeDesc(String status, Pageable pageable);
    Page<Announcement> findAllByOrderByIsTopDescCreateTimeDesc(Pageable pageable);
}
