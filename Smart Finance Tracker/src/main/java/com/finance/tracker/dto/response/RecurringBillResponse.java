package com.finance.tracker.dto.response;

import com.finance.tracker.entity.RecurringBill;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringBillResponse {
    private Long id;
    private Long accountId;
    private String accountName;
    private String billName;
    private BigDecimal amount;
    private RecurringBill.Frequency frequency;
    private LocalDate nextDueDate;
    private Boolean isActive;
    private long daysUntilDue;
    private String urgency; // OVERDUE, DUE_TODAY, DUE_SOON, UPCOMING
}
