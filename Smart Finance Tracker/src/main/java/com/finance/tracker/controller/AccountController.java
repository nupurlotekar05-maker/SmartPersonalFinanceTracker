package com.finance.tracker.controller;

import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.response.AccountResponse;
import com.finance.tracker.dto.response.ApiResponse;
import com.finance.tracker.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> create(@Valid @RequestBody AccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Account created", accountService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAllForCurrentUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> update(@PathVariable Long id,
            @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Account updated", accountService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Account deleted", null));
    }
}
