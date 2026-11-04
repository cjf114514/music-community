package com.music.repository;

import com.music.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    @Query(value = "SELECT sh.* FROM search_history sh WHERE sh.user_id = :userId ORDER BY sh.search_time DESC LIMIT :limit", nativeQuery = true)
    List<SearchHistory> findByUserIdOrderBySearchTimeDesc(@Param("userId") Long userId, @Param("limit") int limit);

    void deleteByUserIdAndKeyword(Long userId, String keyword);

    void deleteByUserId(Long userId);

    @Query(value = "SELECT sh.keyword, COUNT(*) as cnt FROM search_history sh GROUP BY sh.keyword ORDER BY cnt DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findHotKeywords(@Param("limit") int limit);
}
