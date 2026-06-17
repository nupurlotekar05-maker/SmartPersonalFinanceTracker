package com.finance.tracker.repository;

import com.finance.tracker.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);

    void deleteByUserId(Long userId);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    List<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long userId, LocalDate start, LocalDate end);

    List<Transaction> findByUserIdAndTypeOrderByTransactionDateDesc(
            Long userId, Transaction.TransactionType type);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = :type " +
           "AND t.transactionDate BETWEEN :start AND :end")
    BigDecimal sumByUserIdAndTypeAndDateBetween(
            @Param("userId") Long userId,
            @Param("type") Transaction.TransactionType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // Monthly totals for charts
    @Query("SELECT MONTH(t.transactionDate) as month, SUM(t.amount) as total " +
           "FROM Transaction t WHERE t.user.id = :userId AND t.type = :type " +
           "AND YEAR(t.transactionDate) = :year GROUP BY MONTH(t.transactionDate)")
    List<Object[]> monthlyTotals(@Param("userId") Long userId,
                                  @Param("type") Transaction.TransactionType type,
                                  @Param("year") int year);

    // Spending by category
    @Query("SELECT c.name, SUM(t.amount) FROM Transaction t JOIN t.category c " +
           "WHERE t.user.id = :userId AND t.type = 'EXPENSE' " +
           "AND t.transactionDate BETWEEN :start AND :end GROUP BY c.name")
    List<Object[]> spendingByCategory(@Param("userId") Long userId,
                                       @Param("start") LocalDate start,
                                       @Param("end") LocalDate end);

    // For budget tracking
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.category.id = :categoryId AND t.type = 'EXPENSE' " +
           "AND MONTH(t.transactionDate) = :month AND YEAR(t.transactionDate) = :year")
    BigDecimal spentInCategoryThisMonth(@Param("userId") Long userId,
                                         @Param("categoryId") Long categoryId,
                                         @Param("month") int month,
                                         @Param("year") int year);

    // For predictions - last N months average
    @Query(value = "SELECT AVG(monthly_total) FROM (" +
           "SELECT SUM(amount) as monthly_total FROM transactions " +
           "WHERE user_id = :userId AND type = :type " +
           "AND transaction_date >= DATE_SUB(CURDATE(), INTERVAL :months MONTH) " +
           "GROUP BY YEAR(transaction_date), MONTH(transaction_date)) as monthly_data",
           nativeQuery = true)
    BigDecimal averageMonthlyAmount(@Param("userId") Long userId,
                                     @Param("type") String type,
                                     @Param("months") int months);

    List<Transaction> findByUserIdAndTransactionDateBetween(Long userId, LocalDate start, LocalDate end);

    // Category keyword count for auto-categorization
    @Query("SELECT c.name, COUNT(t) FROM Transaction t JOIN t.category c " +
           "WHERE t.user.id = :userId AND LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "GROUP BY c.name ORDER BY COUNT(t) DESC")
    List<Object[]> findCategoryByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    // Admin: all transactions with pagination + optional filters
    @Query("SELECT t FROM Transaction t WHERE " +
           "(:userId IS NULL OR t.user.id = :userId) AND " +
           "(:type IS NULL OR t.type = :type) AND " +
           "(:categoryId IS NULL OR t.category.id = :categoryId) AND " +
           "(:startDate IS NULL OR t.transactionDate >= :startDate) AND " +
           "(:endDate IS NULL OR t.transactionDate <= :endDate) " +
           "ORDER BY t.transactionDate DESC")
    Page<Transaction> findAllWithFilters(
            @Param("userId") Long userId,
            @Param("type") Transaction.TransactionType type,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // Admin dashboard totals
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = :type")
    BigDecimal sumByTypeGlobal(@Param("type") Transaction.TransactionType type);

    // Monthly trend for admin charts (all users)
    @Query("SELECT YEAR(t.transactionDate) as year, MONTH(t.transactionDate) as month, " +
           "t.type as type, SUM(t.amount) as total " +
           "FROM Transaction t WHERE t.transactionDate >= :since " +
           "GROUP BY YEAR(t.transactionDate), MONTH(t.transactionDate), t.type " +
           "ORDER BY YEAR(t.transactionDate), MONTH(t.transactionDate)")
    List<Object[]> monthlyTransactionTrendGlobal(@Param("since") LocalDate since);

    long countByCategoryId(Long categoryId);

    @Query("SELECT COUNT(DISTINCT t.user.id) FROM Transaction t WHERE t.category.id = :categoryId")
    long countDistinctUsersByCategoryId(@Param("categoryId") Long categoryId);
}
