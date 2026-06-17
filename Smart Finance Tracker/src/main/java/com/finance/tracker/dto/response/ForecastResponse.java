package com.finance.tracker.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastResponse {
    private BigDecimal predictedIncome;
    private BigDecimal predictedExpense;
    private BigDecimal predictedSavings;
    private String forecastMonth;
    private List<String> tips;
}
