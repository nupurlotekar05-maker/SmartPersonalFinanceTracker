package com.finance.tracker.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    private long totalUsers;
    private long activeUsers;
    private long suspendedUsers;
    private long totalTransactions;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private List<MonthlyUserGrowth> userGrowth;
    private List<MonthlyTransactionTrend> transactionTrend;
    private long recentFailedLogins;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyUserGrowth {
        private int year;
        private int month;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTransactionTrend {
        private int year;
        private int month;
        private BigDecimal income;
        private BigDecimal expense;
    }
}
