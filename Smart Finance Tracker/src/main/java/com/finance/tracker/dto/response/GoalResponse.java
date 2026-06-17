package com.finance.tracker.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponse {
    private Long id;
    private String goalName;
    private BigDecimal targetAmount;
    private BigDecimal savedAmount;
    private BigDecimal remainingAmount;
    private double progressPercent;
    private LocalDate deadline;
    private long daysRemaining;
    private String status; // IN_PROGRESS, ACHIEVED, OVERDUE
}
