package com.finance.tracker.controller;

import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.response.ApiResponse;
import com.finance.tracker.dto.response.BudgetResponse;
import com.finance.tracker.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> create(@Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Budget created", budgetService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getAll()));
    }

    @GetMapping("/month")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getByMonth(@RequestParam int month,
                                                                          @RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getByMonthYear(month, year)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> update(@PathVariable Long id,
                                                               @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Budget updated", budgetService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Budget deleted", null));
    }
}
