package com.finance.tracker.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GoalRequest {
    @NotBlank private String goalName;

    @NotNull @DecimalMin("0.01")
    private BigDecimal targetAmount;

    private BigDecimal savedAmount = BigDecimal.ZERO;
    private LocalDate deadline;
}
