package com.finance.tracker.dto.response;

import com.finance.tracker.entity.Transaction;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private Long userId;
    private Long accountId;
    private String accountName;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private BigDecimal amount;
    private Transaction.TransactionType type;
    private String description;
    private LocalDate transactionDate;
    private Transaction.PaymentStatus paymentStatus;
    private Transaction.TransactionSource source;
    private String notes;
    private String receiptImageUrl;
    private LocalDateTime createdAt;
}
