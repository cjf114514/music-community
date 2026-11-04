package com.music.repository;

import com.music.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    Page<OperationLog> findAllByOrderByCreateTimeDesc(Pageable pageable);

    @Query("SELECT o FROM OperationLog o WHERE " +
            "(:module IS NULL OR o.module = :module) AND " +
            "(:action IS NULL OR o.action = :action) AND " +
            "(:keyword IS NULL OR o.username LIKE %:keyword% OR o.description LIKE %:keyword%) " +
            "ORDER BY o.createTime DESC")
    Page<OperationLog> findByFilters(@Param("module") String module,
                                     @Param("action") String action,
                                     @Param("keyword") String keyword,
                                     Pageable pageable);
}
