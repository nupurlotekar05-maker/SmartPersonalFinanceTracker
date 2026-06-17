package com.finance.tracker.controller;

import com.finance.tracker.dto.request.ChangePasswordRequest;
import com.finance.tracker.dto.request.ForgotPasswordRequest;
import com.finance.tracker.dto.request.LoginRequest;
import com.finance.tracker.dto.request.RegisterRequest;
import com.finance.tracker.dto.request.ResetPasswordRequest;
import com.finance.tracker.dto.response.ApiResponse;
import com.finance.tracker.dto.response.AuthResponse;
import com.finance.tracker.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ─── Register ─────────────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registered successfully", response));
    }

    // ─── Login ────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    // ─── Get Profile (JWT protected) ──────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        AuthResponse response = authService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", response));
    }

    // ─── Logout ───────────────────────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            authService.logout(userDetails.getUsername());
        }
        return ResponseEntity.ok(ApiResponse.success(
                "Logged out successfully. Please delete the token on the client side.", null));
    }

    // ─── Change Password (JWT protected) ──────────────────────────────────────
    // Available at both POST /api/auth/change-password AND PUT /api/users/change-password
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    // ─── Forgot Password (public — no JWT required) ───────────────────────────
    /**
     * POST /api/auth/forgot-password
     * Body: { "email": "user@example.com" }
     *
     * Always returns success (200) to avoid leaking whether an email exists.
     * Sends a reset link to the registered email with a 15-minute expiry token.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.forgotPassword(request);
        } catch (Exception e) {
            // Log but don't expose — always return generic success
        }
        return ResponseEntity.ok(ApiResponse.success(
                "If that email is registered, you will receive a password reset link shortly.", null));
    }

    // ─── Reset Password (public — token acts as authentication) ───────────────
    /**
     * POST /api/auth/reset-password
     * Body: { "token": "uuid-token", "newPassword": "newPass123" }
     *
     * Validates token, checks expiry, updates password, deletes token.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Password has been reset successfully. Please login with your new password.", null));
    }
}
