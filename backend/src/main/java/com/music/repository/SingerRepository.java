package com.music.repository;

import com.music.entity.Singer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SingerRepository extends JpaRepository<Singer, Long> {
    Optional<Singer> findByName(String name);

    @Query("SELECT s FROM Singer s ORDER BY s.songCount DESC")
    List<Singer> findAllOrderBySongCountDesc();

    org.springframework.data.domain.Page<Singer> findByNameContaining(String name, org.springframework.data.domain.Pageable pageable);
}
