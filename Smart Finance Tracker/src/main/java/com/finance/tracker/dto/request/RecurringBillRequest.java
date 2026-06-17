package com.finance.tracker.dto.request;

import com.finance.tracker.entity.RecurringBill;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecurringBillRequest {
    @NotNull private Long accountId;

    @NotBlank private String billName;

    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull private RecurringBill.Frequency frequency;

    @NotNull private LocalDate nextDueDate;

    private Boolean isActive = true;
}
