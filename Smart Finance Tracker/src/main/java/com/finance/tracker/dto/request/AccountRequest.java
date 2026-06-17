package com.finance.tracker.dto.request;

import com.finance.tracker.entity.Account;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AccountRequest {
    @NotBlank(message = "Account name is required")
    private String accountName;

    @NotNull(message = "Account type is required")
    private Account.AccountType accountType;

    private BigDecimal balance = BigDecimal.ZERO;
}
