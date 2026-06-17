package com.finance.tracker.repository;

import com.finance.tracker.entity.AiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiRequestLogRepository extends JpaRepository<AiRequestLog, Long> {
    List<AiRequestLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}
