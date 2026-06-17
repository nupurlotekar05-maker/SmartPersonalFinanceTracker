package com.finance.tracker.controller;

import com.finance.tracker.dto.request.AdminCategoryRequest;
import com.finance.tracker.dto.request.AdminUpdateUserRequest;
import com.finance.tracker.dto.request.BroadcastRequest;
import com.finance.tracker.dto.response.*;
import com.finance.tracker.service.ActivityLogService;
import com.finance.tracker.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final ActivityLogService activityLogService;

    // ─── DASHBOARD ───────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard data", adminService.getDashboard()));
    }

    // ─── USER MANAGEMENT ─────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AdminUserResponse> users = adminService.getAllUsers(search, status, role, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users fetched", users));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("User profile", adminService.getUserById(userId)));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AdminUserResponse updated = adminService.updateUser(userId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User updated", updated));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        adminService.deleteUser(userId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }

    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<ApiResponse<AdminUserResponse>> suspendUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserDetails userDetails) {
        AdminUserResponse updated = adminService.suspendUser(userId, reason, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User suspended", updated));
    }

    @PostMapping("/users/{userId}/activate")
    public ResponseEntity<ApiResponse<AdminUserResponse>> activateUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        AdminUserResponse updated = adminService.activateUser(userId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User activated", updated));
    }

    // ─── TRANSACTION MONITORING ───────────────────────────

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAllTransactions(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<TransactionResponse> transactions = adminService.getAllTransactions(
                userId, type, categoryId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Transactions fetched", transactions));
    }

    // ─── CATEGORY MANAGEMENT ─────────────────────────────

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getDefaultCategories() {
        return ResponseEntity.ok(ApiResponse.success("Categories fetched", adminService.getAllDefaultCategories()));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody AdminCategoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CategoryResponse created = adminService.createDefaultCategory(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Category created", created));
    }

    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody AdminCategoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CategoryResponse updated = adminService.updateDefaultCategory(categoryId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Category updated", updated));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        adminService.deleteDefaultCategory(categoryId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }

    @PatchMapping("/categories/{categoryId}/toggle")
    public ResponseEntity<ApiResponse<CategoryResponse>> toggleCategory(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        CategoryResponse updated = adminService.toggleDefaultCategoryStatus(categoryId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Category status toggled", updated));
    }

    // ─── ADMIN ACTIONS & SYSTEM ───────────────────────────

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportData() {
        byte[] csvData = adminService.exportDataAsCsv();
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=smartledger_export.csv")
                .body(csvData);
    }

    @DeleteMapping("/logs/clear")
    public ResponseEntity<ApiResponse<Void>> clearLogs(
            @RequestParam(defaultValue = "0") int days,
            @AuthenticationPrincipal UserDetails userDetails) {
        adminService.clearActivityLogs(days, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Logs cleared successfully", null));
    }

    @PostMapping("/notifications/broadcast")
    public ResponseEntity<ApiResponse<Void>> broadcastNotification(
            @Valid @RequestBody BroadcastRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        adminService.broadcastNotification(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Broadcast sent successfully", null));
    }

    // ─── APPROVAL SYSTEM ──────────────────────────────────

    @GetMapping("/actions/pending")
    public ResponseEntity<ApiResponse<List<AdminActionResponse>>> getPendingActions() {
        return ResponseEntity.ok(ApiResponse.success("Pending actions", adminService.getPendingActions()));
    }

    @PostMapping("/actions/request-reset-passwords")
    public ResponseEntity<ApiResponse<AdminActionResponse>> requestResetAllPasswords(
            @AuthenticationPrincipal UserDetails userDetails) {
        AdminActionResponse requested = adminService.requestAction("RESET_ALL_PASSWORDS", null, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Password reset request submitted for approval", requested));
    }

    @PutMapping("/actions/{actionId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveAction(
            @PathVariable Long actionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        adminService.approveAction(actionId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Action approved and executed", null));
    }

    @PutMapping("/actions/{actionId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectAction(
            @PathVariable Long actionId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserDetails userDetails) {
        adminService.rejectAction(actionId, reason, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Action rejected", null));
    }

    // ─── SECURITY & ACTIVITY LOGS ─────────────────────────

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Logs fetched", activityLogService.getAllLogs(pageable)));
    }

    @GetMapping("/logs/login")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getLoginLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Login logs fetched", activityLogService.getLoginLogs(pageable)));
    }

    @GetMapping("/logs/failed-logins")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getFailedLogins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Failed login logs", activityLogService.getFailedLoginLogs(pageable)));
    }

    @GetMapping("/logs/admin-actions")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getAdminActions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Admin action logs", activityLogService.getAdminActionLogs(pageable)));
    }

    @GetMapping("/logs/security-alerts")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getSecurityAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Security alerts", activityLogService.getSecurityAlerts(pageable)));
    }
}
