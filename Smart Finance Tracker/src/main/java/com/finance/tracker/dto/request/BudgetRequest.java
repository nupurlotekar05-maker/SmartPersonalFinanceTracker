package com.finance.tracker.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BudgetRequest {
    @NotNull private Long categoryId;

    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull @Min(1) @Max(12)
    private Integer month;

    @NotNull @Min(2000)
    private Integer year;
}
