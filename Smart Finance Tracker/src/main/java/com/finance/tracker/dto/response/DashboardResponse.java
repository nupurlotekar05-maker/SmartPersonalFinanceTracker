package com.finance.tracker.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    // Summary cards
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netSavings;
    private BigDecimal totalBalance;

    // For line/bar chart: 12 months data
    private List<MonthlyData> monthlyIncome;
    private List<MonthlyData> monthlyExpense;

    // For pie chart: expense by category
    private List<CategoryData> expenseByCategory;

    // Health Score
    private int healthScore;
    private String healthLabel; // POOR, FAIR, GOOD, EXCELLENT
    private List<String> insights;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyData {
        private String month;    // "Jan", "Feb", ...
        private int monthNumber;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryData {
        private String category;
        private BigDecimal amount;
        private double percentage;
    }
}
