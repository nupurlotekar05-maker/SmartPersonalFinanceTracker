package com.finance.tracker.service;

import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.response.BudgetResponse;
import com.finance.tracker.entity.*;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.repository.*;
import com.finance.tracker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final SecurityUtils securityUtils;

    public BudgetResponse create(BudgetRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        User user = securityUtils.getCurrentUser();
        log.info("[BUDGET CREATE] userId={} categoryId={} amount={} month={} year={}",
                userId, request.getCategoryId(), request.getAmount(), request.getMonth(), request.getYear());

        // Prevent duplicate budget for same category/month/year
        budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
                userId, request.getCategoryId(), request.getMonth(), request.getYear()
        ).ifPresent(b -> { throw new BadRequestException("Budget already exists for this category and period"); });

        Category category = categoryRepository.findByIdAndUserIdOrDefault(request.getCategoryId(), userId)
                .orElseThrow(() -> {
                    log.error("[BUDGET CREATE] Category not found or unauthorized: categoryId={} userId={}", request.getCategoryId(), userId);
                    return new ResourceNotFoundException("Category not found with id: " + request.getCategoryId());
                });
        log.debug("[BUDGET CREATE] Category resolved: id={} name={}", category.getId(), category.getName());

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .amount(request.getAmount())
                .month(request.getMonth())
                .year(request.getYear())
                .build();

        Budget saved = budgetRepository.save(budget);
        log.info("[BUDGET CREATE] Saved successfully: budgetId={} userId={}", saved.getId(), userId);
        
        // Trigger check immediately in case existing transactions exceed new budget
        checkBudget(user, category, saved.getMonth(), saved.getYear());
        
        return toBudgetResponse(saved, userId);
    }

    public List<BudgetResponse> getByMonthYear(int month, int year) {
        Long userId = securityUtils.getCurrentUserId();
        return budgetRepository.findByUserIdAndMonthAndYear(userId, month, year)
                .stream().map(b -> toBudgetResponse(b, userId)).collect(Collectors.toList());
    }

    public List<BudgetResponse> getAll() {
        Long userId = securityUtils.getCurrentUserId();
        return budgetRepository.findByUserId(userId)
                .stream().map(b -> toBudgetResponse(b, userId)).collect(Collectors.toList());
    }

    public BudgetResponse update(Long id, BudgetRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        Category category = categoryRepository.findByIdAndUserIdOrDefault(request.getCategoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or unauthorized"));

        budget.setCategory(category);
        budget.setAmount(request.getAmount());
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());

        Budget saved = budgetRepository.save(budget);
        
        // Trigger check immediately in case existing transactions exceed updated budget
        checkBudget(securityUtils.getCurrentUser(), category, saved.getMonth(), saved.getYear());

        return toBudgetResponse(saved, userId);
    }

    public void delete(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        budgetRepository.delete(budget);
    }

    public void checkBudget(User user, Category category, int month, int year) {
        budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(user.getId(), category.getId(), month, year)
            .ifPresent(budget -> {
                BigDecimal spent = transactionRepository.spentInCategoryThisMonth(user.getId(), category.getId(), month, year);
                if (spent == null) spent = BigDecimal.ZERO;
                
                BigDecimal limit = budget.getAmount();
                if (limit.compareTo(BigDecimal.ZERO) <= 0) return;
                
                double pct = spent.divide(limit, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).doubleValue();
                
                if (pct >= 100) {
                    String identifier = category.getName() + " [" + month + "/" + year + "] exceeded";
                    if (!notificationRepository.existsByUserIdAndTitleAndMessageContaining(user.getId(), "Budget Alert", identifier)) {
                        String msg = "Budget Exceeded! You have spent ₹" + spent + " out of ₹" + limit + " for " + identifier;
                        notificationService.createBudgetNotification(user, msg);
                    }
                } else if (pct >= 80) {
                    String identifier = category.getName() + " [" + month + "/" + year + "] used 80%";
                    if (!notificationRepository.existsByUserIdAndTitleAndMessageContaining(user.getId(), "Budget Alert", identifier)) {
                        String msg = "Budget Warning! You have used " + String.format("%.0f", pct) + "% of your budget for " + identifier;
                        notificationService.createBudgetNotification(user, msg);
                    }
                }
            });
    }

    private BudgetResponse toBudgetResponse(Budget b, Long userId) {
        BigDecimal spent = transactionRepository.spentInCategoryThisMonth(
                userId, b.getCategory().getId(), b.getMonth(), b.getYear());
        if (spent == null) spent = BigDecimal.ZERO;

        log.debug("[BUDGET CALC] budgetId={} categoryId={} month={}/{} spentAmount={} budgetLimit={}",
                b.getId(), b.getCategory().getId(), b.getMonth(), b.getYear(), spent, b.getAmount());

        BigDecimal remaining = b.getAmount().subtract(spent);
        double pct = b.getAmount().compareTo(BigDecimal.ZERO) == 0 ? 0
                : spent.divide(b.getAmount(), 4, RoundingMode.HALF_UP).doubleValue() * 100;

        String status = pct >= 100 ? "EXCEEDED" : pct >= 80 ? "WARNING" : "ON_TRACK";

        return BudgetResponse.builder()
                .id(b.getId())
                .categoryId(b.getCategory().getId())
                .categoryName(b.getCategory().getName())
                .budgetAmount(b.getAmount())
                .spentAmount(spent)
                .remainingAmount(remaining)
                .percentageUsed(Math.round(pct * 100.0) / 100.0)
                .month(b.getMonth())
                .year(b.getYear())
                .status(status)
                .build();
    }
}
