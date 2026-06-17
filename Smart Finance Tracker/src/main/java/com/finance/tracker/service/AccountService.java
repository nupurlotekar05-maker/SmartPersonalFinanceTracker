package com.finance.tracker.service;

import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.response.AccountResponse;
import com.finance.tracker.entity.Account;
import com.finance.tracker.entity.User;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final SecurityUtils securityUtils;

    public AccountResponse create(AccountRequest request) {
        User user = securityUtils.getCurrentUser();
        Account account = Account.builder()
                .user(user)
                .accountName(request.getAccountName())
                .accountType(request.getAccountType())
                .balance(request.getBalance())
                .build();
        return toResponse(accountRepository.save(account));
    }

    public List<AccountResponse> getAllForCurrentUser() {
        return accountRepository.findByUserId(securityUtils.getCurrentUserId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public AccountResponse getById(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return toResponse(account);
    }

    public AccountResponse update(Long id, AccountRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        account.setAccountName(request.getAccountName());
        account.setAccountType(request.getAccountType());
        account.setBalance(request.getBalance());
        return toResponse(accountRepository.save(account));
    }

    public void delete(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        accountRepository.delete(account);
    }

    public AccountResponse toResponse(Account a) {
        return AccountResponse.builder()
                .id(a.getId())
                .accountName(a.getAccountName())
                .accountType(a.getAccountType())
                .balance(a.getBalance())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
