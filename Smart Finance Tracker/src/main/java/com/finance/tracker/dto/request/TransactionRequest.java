package com.finance.tracker.dto.request;

import com.finance.tracker.entity.Transaction;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    private Long categoryId; // optional - auto-categorized if null

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private Transaction.TransactionType type;

    @Size(max = 255)
    private String description;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    private Transaction.PaymentStatus paymentStatus = Transaction.PaymentStatus.PAID;
    private Transaction.TransactionSource source = Transaction.TransactionSource.MANUAL;
    private String notes;
}
