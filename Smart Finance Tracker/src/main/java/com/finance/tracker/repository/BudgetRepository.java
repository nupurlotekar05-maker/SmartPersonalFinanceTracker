package com.finance.tracker.repository;

import com.finance.tracker.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserIdAndMonthAndYear(Long userId, int month, int year);
    List<Budget> findByUserId(Long userId);
    void deleteByUserId(Long userId);
    Optional<Budget> findByUserIdAndCategoryIdAndMonthAndYear(Long userId, Long categoryId, int month, int year);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
}
