package com.finance.tracker.repository;

import com.finance.tracker.entity.RecurringBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringBillRepository extends JpaRepository<RecurringBill, Long> {
    List<RecurringBill> findByUserId(Long userId);
    void deleteByUserId(Long userId);
    List<RecurringBill> findByUserIdAndIsActive(Long userId, Boolean isActive);
    Optional<RecurringBill> findByIdAndUserId(Long id, Long userId);
    // For scheduler - all active bills due today or overdue
    List<RecurringBill> findByIsActiveTrueAndNextDueDateLessThanEqual(LocalDate date);
}
