package com.finance.tracker.service;

import com.finance.tracker.dto.response.DashboardResponse;
import com.finance.tracker.dto.response.ForecastResponse;
import com.finance.tracker.entity.Account;
import com.finance.tracker.entity.Transaction;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final SecurityUtils securityUtils;

    private static final String[] MONTH_NAMES = {
        "Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"
    };

    // ── Dashboard ────────────────────────────────────────────────
    public DashboardResponse getDashboard(int year) {
        Long userId = securityUtils.getCurrentUserId();
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end   = LocalDate.of(year, 12, 31);

        BigDecimal totalIncome  = safeSum(transactionRepository.sumByUserIdAndTypeAndDateBetween(
                userId, Transaction.TransactionType.INCOME, start, end));
        BigDecimal totalExpense = safeSum(transactionRepository.sumByUserIdAndTypeAndDateBetween(
                userId, Transaction.TransactionType.EXPENSE, start, end));
        BigDecimal netSavings   = totalIncome.subtract(totalExpense);

        BigDecimal totalBalance = accountRepository.findByUserId(userId)
                .stream().map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Monthly data for line/bar chart
        List<DashboardResponse.MonthlyData> monthlyIncome  = buildMonthlyData(userId, Transaction.TransactionType.INCOME,  year);
        List<DashboardResponse.MonthlyData> monthlyExpense = buildMonthlyData(userId, Transaction.TransactionType.EXPENSE, year);

        // Category breakdown (current year) for pie chart
        List<Object[]> catRaw = transactionRepository.spendingByCategory(userId, start, end);
        BigDecimal catTotal = catRaw.stream()
                .map(r -> (BigDecimal) r[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DashboardResponse.CategoryData> expenseByCategory = catRaw.stream()
                .map(r -> {
                    BigDecimal amt = (BigDecimal) r[1];
                    double pct = catTotal.compareTo(BigDecimal.ZERO) == 0 ? 0
                            : amt.divide(catTotal, 4, RoundingMode.HALF_UP).doubleValue() * 100;
                    return DashboardResponse.CategoryData.builder()
                            .category((String) r[0])
                            .amount(amt)
                            .percentage(Math.round(pct * 100.0) / 100.0)
                            .build();
                })
                .collect(Collectors.toList());

        // Health Score
        int score = computeHealthScore(totalIncome, totalExpense, netSavings);
        String label = score >= 80 ? "EXCELLENT" : score >= 60 ? "GOOD" : score >= 40 ? "FAIR" : "POOR";
        List<String> insights = generateInsights(totalIncome, totalExpense, netSavings, score);

        return DashboardResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netSavings(netSavings)
                .totalBalance(totalBalance)
                .monthlyIncome(monthlyIncome)
                .monthlyExpense(monthlyExpense)
                .expenseByCategory(expenseByCategory)
                .healthScore(score)
                .healthLabel(label)
                .insights(insights)
                .build();
    }

    // ── Forecast ─────────────────────────────────────────────────
    public ForecastResponse getForecast() {
        Long userId = securityUtils.getCurrentUserId();

        BigDecimal avgIncome  = safeSum(transactionRepository.averageMonthlyAmount(userId, "INCOME",  6));
        BigDecimal avgExpense = safeSum(transactionRepository.averageMonthlyAmount(userId, "EXPENSE", 6));
        BigDecimal avgSavings = avgIncome.subtract(avgExpense);

        String nextMonth = LocalDate.now().plusMonths(1)
                .getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        List<String> tips = buildForecastTips(avgIncome, avgExpense, avgSavings);

        return ForecastResponse.builder()
                .predictedIncome(avgIncome.setScale(2, RoundingMode.HALF_UP))
                .predictedExpense(avgExpense.setScale(2, RoundingMode.HALF_UP))
                .predictedSavings(avgSavings.setScale(2, RoundingMode.HALF_UP))
                .forecastMonth(nextMonth)
                .tips(tips)
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────
    private List<DashboardResponse.MonthlyData> buildMonthlyData(Long userId,
            Transaction.TransactionType type, int year) {

        List<Object[]> raw = transactionRepository.monthlyTotals(userId, type, year);
        Map<Integer, BigDecimal> byMonth = new HashMap<>();
        for (Object[] row : raw) {
            byMonth.put(((Number) row[0]).intValue(), (BigDecimal) row[1]);
        }

        List<DashboardResponse.MonthlyData> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            result.add(DashboardResponse.MonthlyData.builder()
                    .month(MONTH_NAMES[m - 1])
                    .monthNumber(m)
                    .amount(byMonth.getOrDefault(m, BigDecimal.ZERO))
                    .build());
        }
        return result;
    }

    private int computeHealthScore(BigDecimal income, BigDecimal expense, BigDecimal savings) {
        if (income.compareTo(BigDecimal.ZERO) == 0) return 0;

        // Savings rate (0-50 pts)
        double savingsRate = savings.divide(income, 4, RoundingMode.HALF_UP).doubleValue();
        int savingsScore   = (int) Math.min(savingsRate * 200, 50); // 25% saving → 50 pts

        // Expense ratio (0-30 pts)
        double expenseRatio = expense.divide(income, 4, RoundingMode.HALF_UP).doubleValue();
        int expenseScore    = expenseRatio <= 0.5 ? 30 : expenseRatio <= 0.7 ? 20
                            : expenseRatio <= 0.9 ? 10 : 0;

        // Base (20 pts for having income > 0)
        return Math.min(savingsScore + expenseScore + 20, 100);
    }

    private List<String> generateInsights(BigDecimal income, BigDecimal expense,
                                           BigDecimal savings, int score) {
        List<String> insights = new ArrayList<>();

        if (income.compareTo(BigDecimal.ZERO) == 0) {
            insights.add("No income recorded yet. Start by adding transactions.");
            return insights;
        }

        double savingsRate = savings.divide(income, 4, RoundingMode.HALF_UP).doubleValue() * 100;
        double expenseRate = expense.divide(income, 4, RoundingMode.HALF_UP).doubleValue() * 100;

        if (savingsRate >= 20) insights.add("Great job! You are saving " + String.format("%.1f", savingsRate) + "% of your income.");
        else                   insights.add("Try to save at least 20% of your income. Currently at " + String.format("%.1f", savingsRate) + "%.");

        if (expenseRate > 90)  insights.add("Warning: Your expenses are " + String.format("%.1f", expenseRate) + "% of income. Cut non-essentials.");
        else if (expenseRate > 70) insights.add("Expenses are " + String.format("%.1f", expenseRate) + "% of income. Room to improve.");

        if (score >= 80)       insights.add("Excellent financial health! Keep it up.");
        else if (score >= 60)  insights.add("Good financial health. Focus on growing savings.");
        else                   insights.add("Financial health needs attention. Review your budget.");

        return insights;
    }

    private List<String> buildForecastTips(BigDecimal income, BigDecimal expense, BigDecimal savings) {
        List<String> tips = new ArrayList<>();
        if (savings.compareTo(BigDecimal.ZERO) < 0) {
            tips.add("You are projected to spend more than you earn next month. Review your expenses.");
        } else {
            tips.add("You are projected to save ₹" + savings.setScale(0, RoundingMode.HALF_UP) + " next month.");
        }
        tips.add("Based on last 6 months average spending patterns.");
        tips.add("Consider setting a budget for your top expense categories.");
        return tips;
    }

    private BigDecimal safeSum(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
