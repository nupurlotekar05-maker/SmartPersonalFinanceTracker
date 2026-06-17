package com.finance.tracker.service;

import com.finance.tracker.dto.request.AiChatRequest;
import com.finance.tracker.dto.response.AiChatResponse;
import com.finance.tracker.entity.AiRequestLog;
import com.finance.tracker.entity.Budget;
import com.finance.tracker.entity.Goal;
import com.finance.tracker.entity.Transaction;
import com.finance.tracker.entity.User;
import com.finance.tracker.repository.AiRequestLogRepository;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.GoalRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;

/**
 * AiChatService — uses Google Gemini 1.5 Flash (FREE) for intelligent responses.
 *
 * CHANGE: Previously used hardcoded keyword matching (if/else logic).
 *         Now uses real Gemini AI with the user's financial data as context,
 *         allowing natural language questions and richer answers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final AiRequestLogRepository aiLogRepository;
    private final SecurityUtils securityUtils;
    private final GeminiService geminiService;

    public AiChatResponse chat(AiChatRequest request) {
        try {
            User user = securityUtils.getCurrentUser();
            String prompt = request.getMessage().trim();

            // Build financial context summary for Gemini
            String context = buildFinancialContext(user.getId());

            // Call Gemini AI with user's financial data as context
            String response = geminiService.chat(prompt, context);

            // Log the interaction
            aiLogRepository.save(AiRequestLog.builder()
                    .user(user)
                    .prompt(prompt)
                    .response(response)
                    .tokensUsed(response != null ? response.split("\\s+").length : 0)
                    .build());

            return AiChatResponse.builder()
                    .success(true)
                    .reply(response)
                    .build();
        } catch (Exception e) {
            log.error("AI Chat Service Error: {}", e.getMessage(), e);
            return AiChatResponse.builder()
                    .success(false)
                    .message("AI Assistant Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Build a text summary of the user's financial data to pass as context to Gemini.
     */
    private String buildFinancialContext(Long userId) {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year  = now.getYear();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth   = now.withDayOfMonth(now.lengthOfMonth());

        StringBuilder ctx = new StringBuilder();
        ctx.append("Current month: ").append(now.getMonth()).append(" ").append(year).append("\n");

        // Monthly income & expense
        BigDecimal income  = safeVal(transactionRepository.sumByUserIdAndTypeAndDateBetween(
                userId, Transaction.TransactionType.INCOME,  startOfMonth, endOfMonth));
        BigDecimal expense = safeVal(transactionRepository.sumByUserIdAndTypeAndDateBetween(
                userId, Transaction.TransactionType.EXPENSE, startOfMonth, endOfMonth));

        ctx.append("This month income:  Rs.").append(income.setScale(2, RoundingMode.HALF_UP)).append("\n");
        ctx.append("This month expense: Rs.").append(expense.setScale(2, RoundingMode.HALF_UP)).append("\n");
        ctx.append("Net savings:        Rs.").append(income.subtract(expense).setScale(2, RoundingMode.HALF_UP)).append("\n\n");

        // Budget status
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);
        if (!budgets.isEmpty()) {
            ctx.append("Budget status:\n");
            for (Budget b : budgets) {
                BigDecimal spent = safeVal(transactionRepository.spentInCategoryThisMonth(
                        userId, b.getCategory().getId(), month, year));
                double pct = b.getAmount().compareTo(BigDecimal.ZERO) == 0 ? 0
                        : spent.divide(b.getAmount(), 4, RoundingMode.HALF_UP).doubleValue() * 100;
                ctx.append(String.format("  %s: Rs.%.0f spent of Rs.%.0f budget (%.0f%%)%s\n",
                        b.getCategory().getName(),
                        spent.doubleValue(),
                        b.getAmount().doubleValue(),
                        pct,
                        pct >= 100 ? " [EXCEEDED]" : pct >= 80 ? " [WARNING]" : ""));
            }
            ctx.append("\n");
        }

        // Goals
        List<Goal> goals = goalRepository.findByUserId(userId);
        if (!goals.isEmpty()) {
            ctx.append("Savings goals:\n");
            for (Goal g : goals) {
                double pct = g.getTargetAmount().compareTo(BigDecimal.ZERO) == 0 ? 0
                        : g.getSavedAmount().divide(g.getTargetAmount(), 4, RoundingMode.HALF_UP).doubleValue() * 100;
                ctx.append(String.format("  %s: Rs.%.0f saved of Rs.%.0f target (%.0f%%)%s\n",
                        g.getGoalName(),
                        g.getSavedAmount().doubleValue(),
                        g.getTargetAmount().doubleValue(),
                        pct,
                        g.getDeadline() != null ? " deadline: " + g.getDeadline() : ""));
            }
        }

        return ctx.toString();
    }

    private BigDecimal safeVal(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
