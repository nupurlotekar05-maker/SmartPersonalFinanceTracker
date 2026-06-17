package com.finance.tracker.controller;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.ApiResponse;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.service.TransactionService;
import com.finance.tracker.service.OcrService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final OcrService ocrService;

    // Create income or expense (Quick Entry)
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> create(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Transaction added", transactionService.create(request)));
    }

    // List all transactions
    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getAll()));
    }

    // List with date filter
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> filter(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getByDateRange(start, end)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> update(@PathVariable Long id,
                                                                    @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Transaction updated", transactionService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted", null));
    }

    // Upload receipt image to a transaction
    @PostMapping("/{id}/receipt")
    public ResponseEntity<ApiResponse<TransactionResponse>> uploadReceipt(@PathVariable Long id,
                                                                           @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Receipt uploaded", transactionService.uploadReceipt(id, file)));
    }

    // Scan receipt and return parsed data
    @PostMapping("/ocr-scan")
    public ResponseEntity<ApiResponse<OcrService.OcrResult>> scanReceipt(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Receipt scanned", ocrService.extractFromReceipt(file)));
    }
}
