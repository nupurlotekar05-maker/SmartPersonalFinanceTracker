package com.finance.tracker.controller;

import com.finance.tracker.dto.response.ApiResponse;
import com.finance.tracker.dto.response.DashboardResponse;
import com.finance.tracker.dto.response.ForecastResponse;
import com.finance.tracker.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // Full dashboard (income, expense, charts, health score, insights)
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @RequestParam(defaultValue = "0") int year) {
        if (year == 0) year = LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getDashboard(year)));
    }

    // Smart predictions for next month
    @GetMapping("/forecast")
    public ResponseEntity<ApiResponse<ForecastResponse>> getForecast() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getForecast()));
    }
}
