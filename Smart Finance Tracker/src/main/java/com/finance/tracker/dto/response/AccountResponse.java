package com.finance.tracker.dto.response;

import com.finance.tracker.entity.Account;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private Long id;
    private String accountName;
    private Account.AccountType accountType;
    private BigDecimal balance;
    private LocalDateTime createdAt;
}
