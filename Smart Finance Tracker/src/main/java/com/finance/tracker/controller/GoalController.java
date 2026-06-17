package com.finance.tracker.controller;

import com.finance.tracker.dto.request.GoalRequest;
import com.finance.tracker.dto.response.ApiResponse;
import com.finance.tracker.dto.response.GoalResponse;
import com.finance.tracker.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public ResponseEntity<ApiResponse<GoalResponse>> create(@Valid @RequestBody GoalRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Goal created", goalService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(goalService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GoalResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(goalService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GoalResponse>> update(@PathVariable Long id,
                                                             @Valid @RequestBody GoalRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Goal updated", goalService.update(id, request)));
    }

    // Add money toward a goal
    @PatchMapping("/{id}/contribute")
    public ResponseEntity<ApiResponse<GoalResponse>> contribute(@PathVariable Long id,
                                                                 @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success("Contribution added", goalService.contribute(id, amount)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        goalService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Goal deleted", null));
    }
}
