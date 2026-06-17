package com.finance.tracker.repository;

import com.finance.tracker.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByUserId(Long userId);
    Page<ActivityLog> findByLogTypeOrderByCreatedAtDesc(ActivityLog.LogType logType, Pageable pageable);

    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM ActivityLog a WHERE a.logType IN :types ORDER BY a.createdAt DESC")
    Page<ActivityLog> findByLogTypeIn(@Param("types") List<ActivityLog.LogType> types, Pageable pageable);

    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.logType = 'LOGIN_FAILED' AND a.createdAt >= :since")
    long countFailedLoginsAfter(@Param("since") LocalDateTime since);

    List<ActivityLog> findTop10ByOrderByCreatedAtDesc();

    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
