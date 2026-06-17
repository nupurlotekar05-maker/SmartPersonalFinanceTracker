package com.finance.tracker.controller;

import com.finance.tracker.dto.request.ChangePasswordRequest;
import com.finance.tracker.dto.response.ApiResponse;
import com.finance.tracker.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * UserController — exposes user self-service endpoints under /api/users.
 *
 * PUT /api/users/change-password — requires a valid JWT (authenticated user only).
 * Delegates all logic to AuthService.changePassword().
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    /**
     * PUT /api/users/change-password
     *
     * Allows an authenticated user to change their own password.
     * Validates:
     *   - currentPassword matches BCrypt hash in DB
     *   - newPassword != currentPassword
     *   - newPassword == confirmPassword
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}
