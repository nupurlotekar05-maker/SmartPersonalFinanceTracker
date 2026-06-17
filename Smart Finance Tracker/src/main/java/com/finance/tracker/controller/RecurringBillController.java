package com.finance.tracker.controller;

import com.finance.tracker.dto.request.RecurringBillRequest;
import com.finance.tracker.dto.response.ApiResponse;
import com.finance.tracker.dto.response.RecurringBillResponse;
import com.finance.tracker.service.RecurringBillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class RecurringBillController {

    private final RecurringBillService billService;

    @PostMapping
    public ResponseEntity<ApiResponse<RecurringBillResponse>> create(@Valid @RequestBody RecurringBillRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bill created", billService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecurringBillResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(billService.getAll()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<RecurringBillResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(billService.getActive()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringBillResponse>> update(@PathVariable Long id,
                                                                      @Valid @RequestBody RecurringBillRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bill updated", billService.update(id, request)));
    }

    // Mark bill as paid and advance due date
    @PatchMapping("/{id}/paid")
    public ResponseEntity<ApiResponse<RecurringBillResponse>> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Bill marked as paid, next due date updated",
                billService.advanceDueDate(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        billService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Bill deleted", null));
    }
}
