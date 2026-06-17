package com.finance.tracker.service;

import com.finance.tracker.dto.request.ChangePasswordRequest;
import com.finance.tracker.dto.request.ForgotPasswordRequest;
import com.finance.tracker.dto.request.LoginRequest;
import com.finance.tracker.dto.request.RegisterRequest;
import com.finance.tracker.dto.request.ResetPasswordRequest;
import com.finance.tracker.dto.response.AuthResponse;
import com.finance.tracker.entity.ActivityLog;
import com.finance.tracker.entity.PasswordResetToken;
import com.finance.tracker.entity.Role;
import com.finance.tracker.entity.User;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.repository.PasswordResetTokenRepository;
import com.finance.tracker.repository.RoleRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FIXES APPLIED:
 *
 * 1. generateToken() now receives the role name as the second argument.
 *    This embeds "role":"ADMIN" (or "USER") in the JWT payload.
 *    The frontend can decode the JWT and read the role without an extra API call.
 *
 * 2. getProfile() no longer returns a token (token = null is fine for /me endpoint).
 *
 * 3. Kept all existing account-lock / failed-attempt logic unchanged.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ActivityLogService activityLogService;
    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        Role userRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new BadRequestException("Default role not found. Please run seed.sql."));

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .status(User.UserStatus.ACTIVE)
                .isVerified(false)
                .failedAttempts(0)
                .accountLocked(false)
                .lockTime(null)
                .build();

        userRepository.save(user);

        // FIX: pass role name to generateToken so JWT carries the role claim
        String token = jwtUtil.generateToken(user.getEmail(), userRole.getRoleName());

        activityLogService.log(user, "REGISTER",
                "New user registered: " + user.getEmail(),
                null, ActivityLog.LogType.USER_ACTION);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(userRole.getRoleName())   // "USER" or "ADMIN"
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            // authenticationManager.authenticate() will call CustomUserDetailsService.loadUserByUsername()
            // which now correctly sets enabled=false for suspended/blocked users, causing:
            //   - DisabledException   → user is SUSPENDED or BLOCKED
            //   - LockedException     → user has accountLocked = true
            //   - BadCredentialsException → wrong password
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (DisabledException e) {
            // SUSPENDED or BLOCKED user — log LOGIN_BLOCKED with correct type
            userRepository.findByEmail(request.getEmail()).ifPresent(user ->
                activityLogService.log(user, "LOGIN_BLOCKED",
                        "Login attempt by suspended/blocked user: " + user.getEmail(),
                        null, ActivityLog.LogType.LOGIN_BLOCKED)
            );
            throw new BadRequestException("Your account has been suspended. Please contact support.");

        } catch (LockedException e) {
            // Account locked after too many failed attempts
            userRepository.findByEmail(request.getEmail()).ifPresent(user ->
                activityLogService.log(user, "LOGIN_BLOCKED",
                        "Login attempt on locked account: " + user.getEmail(),
                        null, ActivityLog.LogType.LOGIN_BLOCKED)
            );
            throw new BadRequestException("Account is locked after too many failed attempts. Please contact support.");

        } catch (AuthenticationException e) {
            // Wrong password — log LOGIN_FAILED and increment failed attempts
            userRepository.findByEmail(request.getEmail()).ifPresentOrElse(user -> {
                int attempts = user.getFailedAttempts() == null ? 0 : user.getFailedAttempts();
                attempts++;
                user.setFailedAttempts(attempts);
                if (attempts >= 5) {
                    user.setAccountLocked(true);
                    user.setLockTime(java.time.LocalDateTime.now());
                    activityLogService.log(user, "ACCOUNT_LOCKED",
                            "Account locked after 5 failed login attempts: " + user.getEmail(),
                            null, ActivityLog.LogType.SECURITY_ALERT);
                }
                userRepository.save(user);
                activityLogService.log(user, "LOGIN_FAILED",
                        "Failed login attempt for: " + user.getEmail(),
                        null, ActivityLog.LogType.LOGIN_FAILED);
            }, () -> activityLogService.logAnonymous("LOGIN_FAILED",
                    "Failed login for unknown email: " + request.getEmail(),
                    null, ActivityLog.LogType.LOGIN_FAILED));

            throw new BadRequestException("Invalid email or password");
        }

        // Reaching here means authentication fully succeeded: user is ACTIVE, not locked, password correct
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Reset failed attempts on successful login
        if (user.getFailedAttempts() != null && user.getFailedAttempts() > 0) {
            user.setFailedAttempts(0);
            userRepository.save(user);
        }

        String roleName = user.getRole().getRoleName();
        String token = jwtUtil.generateToken(user.getEmail(), roleName);

        // LOGIN_SUCCESS is only logged here — after ALL checks pass
        activityLogService.log(user, "LOGIN_SUCCESS",
                "Successful login: " + user.getEmail(),
                null, ActivityLog.LogType.LOGIN_SUCCESS);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(roleName)
                .build();
    }

    public AuthResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        return AuthResponse.builder()
                .type("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().getRoleName())
                .build();
    }

    public void logout(String email) {
        userRepository.findByEmail(email).ifPresent(user ->
                activityLogService.log(user, "LOGOUT",
                        "User logged out: " + email,
                        null, ActivityLog.LogType.LOGOUT));
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current password");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.deleteAllByUser(user);

        activityLogService.log(user, "CHANGE_PASSWORD",
                "Password changed for: " + email,
                null, ActivityLog.LogType.USER_ACTION);
    }

    // =========================================================
    // FORGOT PASSWORD — Generate token & send reset email
    // =========================================================
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Password reset requested for email: {}", email);

        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            // Delete any existing reset tokens for this user
            passwordResetTokenRepository.deleteAllByUser(user);
            log.debug("Deleted existing reset tokens for user: {}", email);

            // Generate a secure UUID token
            String token = UUID.randomUUID().toString();
            
            // Save to password_reset_tokens table with 15 minute expiry
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiryDate(LocalDateTime.now().plusMinutes(15))
                    .build();
            
            passwordResetTokenRepository.saveAndFlush(resetToken);
            log.info("New reset token saved for user: {}", email);

            // Send the reset email - catch exceptions so it doesn't rollback the DB save!
            try {
                emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);
                log.info("Password reset email successfully sent to: {}", email);
            } catch (Exception e) {
                log.error("Failed to send reset email to {}. Token is saved in DB, user can still use manual link. Error: {}", email, e.getMessage());
            }

            activityLogService.log(user, "FORGOT_PASSWORD_REQUEST",
                    "Password reset link generated for: " + email,
                    null, ActivityLog.LogType.USER_ACTION);
        }, () -> {
            log.warn("Password reset attempted for non-existent email: {}", email);
        });
    }

    // =========================================================
    // RESET PASSWORD — Validate token and update password
    // =========================================================
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Attempting password reset with token: {}", request.getToken());

        // Find token in DB
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> {
                    log.warn("Invalid or non-existent reset token used: {}", request.getToken());
                    return new BadRequestException("Invalid or expired reset token");
                });

        // Check token expiry
        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            log.warn("Expired reset token used and deleted: {}", request.getToken());
            throw new BadRequestException("Reset token has expired. Please request a new password reset.");
        }

        // Update password
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password successfully updated for user: {}", user.getEmail());

        // Delete the used token (one-time use)
        passwordResetTokenRepository.delete(resetToken);
        log.debug("One-time use token deleted: {}", request.getToken());

        activityLogService.log(user, "PASSWORD_RESET_SUCCESS",
                "Password was successfully reset using token flow",
                null, ActivityLog.LogType.USER_ACTION);
    }
}
