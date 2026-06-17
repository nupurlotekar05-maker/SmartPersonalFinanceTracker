package com.finance.tracker.service;

import com.finance.tracker.dto.request.AdminCategoryRequest;
import com.finance.tracker.dto.request.AdminUpdateUserRequest;
import com.finance.tracker.dto.response.*;
import com.finance.tracker.entity.*;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FIXES APPLIED:
 *
 * 1. getDashboard() — added null-safe handling for sumByType() which returns
 *    null when no transactions exist (empty DB). Without this, the dashboard
 *    API throws NullPointerException and returns 500.
 *
 * 2. getAllTransactions() — the JPQL query param :type must be a proper enum
 *    value or null. Added explicit null-guard so passing type=null works.
 *
 * 3. toTransactionResponse() — added null-check for category (category is
 *    nullable in the Transaction entity). Without this, mapping throws NPE.
 *
 * 4. Added @Slf4j for diagnostic logging — critical for production debugging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final RecurringBillRepository recurringBillRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    // ─────────────────────────────────────────────────────
    // DASHBOARD
    // ─────────────────────────────────────────────────────

    public AdminDashboardResponse getDashboard() {
        long totalUsers        = userRepository.count();
        long activeUsers       = userRepository.countByStatus(User.UserStatus.ACTIVE);
        long suspendedUsers    = userRepository.countByStatus(User.UserStatus.BLOCKED);
        long totalTransactions = transactionRepository.count();

        // FIX: sumByType returns null when no rows match; default to ZERO
        BigDecimal totalIncome  = Optional.ofNullable(
                transactionRepository.sumByTypeGlobal(Transaction.TransactionType.INCOME))
                .orElse(BigDecimal.ZERO);
        BigDecimal totalExpense = Optional.ofNullable(
                transactionRepository.sumByTypeGlobal(Transaction.TransactionType.EXPENSE))
                .orElse(BigDecimal.ZERO);

        // User growth: last 12 months
        LocalDateTime twelveMonthsAgo = LocalDateTime.now().minusMonths(12);
        List<Object[]> growthRows = userRepository.monthlyUserGrowth(twelveMonthsAgo);
        List<AdminDashboardResponse.MonthlyUserGrowth> userGrowth = growthRows.stream()
                .map(row -> AdminDashboardResponse.MonthlyUserGrowth.builder()
                        .month(((Number) row[0]).intValue())
                        .year(((Number) row[1]).intValue())
                        .count(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        // Transaction trend: last 12 months
        LocalDate twelveMonthsAgoDate = LocalDate.now().minusMonths(12);
        List<Object[]> trendRows = transactionRepository.monthlyTransactionTrendGlobal(twelveMonthsAgoDate);

        // Aggregate by year-month key
        Map<String, AdminDashboardResponse.MonthlyTransactionTrend> trendMap = new LinkedHashMap<>();
        for (Object[] row : trendRows) {
            int    year   = ((Number) row[0]).intValue();
            int    month  = ((Number) row[1]).intValue();
            String key    = year + "-" + month;
            Transaction.TransactionType type   = Transaction.TransactionType.valueOf(row[2].toString());
            // FIX: guard null amount (e.g. when only one type has data)
            BigDecimal amount = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;

            trendMap.putIfAbsent(key, AdminDashboardResponse.MonthlyTransactionTrend.builder()
                    .year(year).month(month)
                    .income(BigDecimal.ZERO).expense(BigDecimal.ZERO).build());

            AdminDashboardResponse.MonthlyTransactionTrend trend = trendMap.get(key);
            if (type == Transaction.TransactionType.INCOME) {
                trend.setIncome(amount);
            } else {
                trend.setExpense(amount);
            }
        }

        long recentFailedLogins = activityLogService.countRecentFailedLogins();

        log.debug("Admin dashboard: totalUsers={}, totalTransactions={}, income={}, expense={}",
                totalUsers, totalTransactions, totalIncome, totalExpense);

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .suspendedUsers(suspendedUsers)
                .totalTransactions(totalTransactions)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .userGrowth(userGrowth)
                .transactionTrend(new ArrayList<>(trendMap.values()))
                .recentFailedLogins(recentFailedLogins)
                .build();
    }

    // ─────────────────────────────────────────────────────
    // USER MANAGEMENT
    // ─────────────────────────────────────────────────────

    public Page<AdminUserResponse> getAllUsers(String search, String status, String role, Pageable pageable) {
        Page<User> users;
        if (search != null && !search.isBlank()) {
            users = userRepository.searchUsers(search.trim(), pageable);
        } else if (status != null && !status.isBlank()) {
            try {
                users = userRepository.findByStatus(User.UserStatus.valueOf(status.toUpperCase()), pageable);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status value: " + status);
            }
        } else if (role != null && !role.isBlank()) {
            users = userRepository.findByRoleName(role.toUpperCase(), pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(this::toUserResponse);
    }

    public AdminUserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return toUserResponse(user);
    }

    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUpdateUserRequest request, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (request.getName() != null) user.setName(request.getName());
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getStatus() != null) {
            try {
                user.setStatus(User.UserStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + request.getStatus());
            }
        }
        if (request.getRole() != null) {
            Role newRole = roleRepository.findByRoleName(request.getRole().toUpperCase())
                    .orElseThrow(() -> new BadRequestException("Role not found: " + request.getRole()));
            user.setRole(newRole);
        }
        if (request.getIsVerified() != null) user.setIsVerified(request.getIsVerified());

        userRepository.save(user);

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        activityLogService.log(admin, "UPDATE_USER",
                "Admin updated user #" + userId + " (" + user.getEmail() + ")",
                null, ActivityLog.LogType.ADMIN_ACTION);

        return toUserResponse(user);
    }

    @Transactional
    public void deleteUser(Long userId, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Validation: Cannot delete an ADMIN user (System Protection)
        if ("ADMIN".equals(user.getRole().getRoleName())) {
            log.warn("Attempt to delete ADMIN user #{} blocked.", userId);
            throw new BadRequestException("System protection: Admin accounts cannot be deleted.");
        }

        String deletedEmail = user.getEmail();
        log.info("Cleaning up data for user #{} ({}) before deletion.", userId, deletedEmail);

        // Cleanup associated data in order to satisfy FK constraints
        activityLogRepository.deleteByUserId(userId);
        notificationRepository.deleteByUserId(userId);
        recurringBillRepository.deleteByUserId(userId);
        budgetRepository.deleteByUserId(userId);
        transactionRepository.deleteByUserId(userId);
        goalRepository.deleteByUserId(userId);
        accountRepository.deleteByUserId(userId);
        categoryRepository.deleteByUserId(userId); // Only deletes user-specific categories

        userRepository.delete(user);

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        activityLogService.log(admin, "DELETE_USER",
                "Admin deleted user #" + userId + " (" + deletedEmail + ")",
                null, ActivityLog.LogType.ADMIN_ACTION);
        
        log.info("User #{} and all associated data deleted successfully by admin {}.", userId, adminEmail);
    }

    @Transactional
    public AdminUserResponse suspendUser(Long userId, String reason, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        user.setStatus(User.UserStatus.SUSPENDED);
        user.setSuspensionReason(reason != null ? reason : "No reason provided by admin");
        user.setSuspendedAt(LocalDateTime.now());
        userRepository.save(user);

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        activityLogService.log(admin, "SUSPEND_USER",
                "Admin suspended user #" + userId + " (" + user.getEmail() + "). Reason: " + user.getSuspensionReason(),
                null, ActivityLog.LogType.ADMIN_ACTION);

        return toUserResponse(user);
    }

    @Transactional
    public AdminUserResponse activateUser(Long userId, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setStatus(User.UserStatus.ACTIVE);
        user.setSuspensionReason(null);
        user.setSuspendedAt(null);
        user.setAccountLocked(false);
        user.setFailedAttempts(0);
        userRepository.save(user);

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        activityLogService.log(admin, "ACTIVATE_USER",
                "Admin activated user #" + userId + " (" + user.getEmail() + ")",
                null, ActivityLog.LogType.ADMIN_ACTION);

        return toUserResponse(user);
    }

    // ─── ADMIN ACTIONS & SYSTEM ───────────────────────────

    public byte[] exportDataAsCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("Type,ID,User,Amount,Date,Description\n");
        
        List<User> users = userRepository.findAll();
        for (User u : users) {
            csv.append(String.format("USER,%d,%s,0,%s,%s\n", u.getId(), u.getEmail(), u.getCreatedAt(), u.getName()));
        }
        
        List<Transaction> txs = transactionRepository.findAll();
        for (Transaction t : txs) {
            csv.append(String.format("TXN,%d,%s,%s,%s,%s\n", t.getId(), t.getUser().getEmail(), t.getAmount(), t.getTransactionDate(), t.getDescription()));
        }
        
        return csv.toString().getBytes();
    }

    @Transactional
    public void clearActivityLogs(int days, String adminEmail) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        
        // Clear managed ActivityLog (which includes login logs internally via enum)
        activityLogRepository.deleteByCreatedAtBefore(cutoff);
        
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        activityLogService.log(admin, "CLEAR_LOGS", "Admin cleared system logs", null, ActivityLog.LogType.ADMIN_ACTION);
    }

    @Transactional
    public void broadcastNotification(com.finance.tracker.dto.request.BroadcastRequest request, String adminEmail) {
        Notification.NotificationType notificationType = Notification.NotificationType.SYSTEM;
        if (request.getType() != null && !request.getType().isBlank()) {
            try {
                notificationType = Notification.NotificationType.valueOf(request.getType().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Default to SYSTEM if invalid
            }
        }

        final Notification.NotificationType finalType = notificationType;
        List<User> users = userRepository.findAll();
        List<Notification> notifications = users.stream().map(u -> Notification.builder()
                .user(u)
                .title(request.getTitle())
                .message(request.getMessage())
                .type(finalType)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build()).collect(Collectors.toList());
        
        notificationRepository.saveAll(notifications);
        
        // Push real-time broadcast
        notificationService.broadcast(request.getTitle(), request.getMessage(), finalType);
        
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        activityLogService.log(admin, "BROADCAST", "Admin broadcasted message: " + request.getTitle(), null, ActivityLog.LogType.ADMIN_ACTION);
    }

    // ─── APPROVAL SYSTEM ──────────────────────────────────

    private final AdminActionRepository adminActionRepository;

    public List<AdminActionResponse> getPendingActions() {
        return adminActionRepository.findByStatus(AdminAction.ActionStatus.PENDING).stream()
                .map(this::toActionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminActionResponse requestAction(String type, String metadata, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        AdminAction action = AdminAction.builder()
                .type(AdminAction.ActionType.valueOf(type))
                .requestedBy(admin)
                .metadata(metadata)
                .status(AdminAction.ActionStatus.PENDING)
                .build();

        return toActionResponse(adminActionRepository.save(action));
    }

    @Transactional
    public void approveAction(Long actionId, String adminEmail) {
        AdminAction action = adminActionRepository.findById(actionId)
                .orElseThrow(() -> new ResourceNotFoundException("Action not found"));
        
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        
        action.setStatus(AdminAction.ActionStatus.APPROVED);
        action.setApprovedBy(admin);
        
        // Execute the action logic
        executeAction(action);
        
        action.setStatus(AdminAction.ActionStatus.EXECUTED);
        action.setExecutedAt(LocalDateTime.now());
        adminActionRepository.save(action);

        activityLogService.log(admin, "APPROVE_ACTION", "Admin approved and executed action: " + action.getType(), null, ActivityLog.LogType.ADMIN_ACTION);
    }

    @Transactional
    public void rejectAction(Long actionId, String reason, String adminEmail) {
        AdminAction action = adminActionRepository.findById(actionId)
                .orElseThrow(() -> new ResourceNotFoundException("Action not found"));
        
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        
        action.setStatus(AdminAction.ActionStatus.REJECTED);
        action.setApprovedBy(admin);
        action.setReason(reason);
        adminActionRepository.save(action);

        activityLogService.log(admin, "REJECT_ACTION", "Admin rejected action: " + action.getType() + ". Reason: " + reason, null, ActivityLog.LogType.ADMIN_ACTION);
    }

    private void executeAction(AdminAction action) {
        switch (action.getType()) {
            case RESET_ALL_PASSWORDS:
                resetAllUserPasswords();
                break;
            case CLEAR_LOGS:
                // logic here if needed
                break;
            default:
                log.warn("Execution logic not implemented for action type: {}", action.getType());
        }
    }

    private void resetAllUserPasswords() {
        List<User> users = userRepository.findAll();
        String defaultHashedPassword = "{noop}Reset123!"; // Simplified for demo; should be dynamic
        for (User u : users) {
            if (!"ADMIN".equals(u.getRole().getRoleName())) {
                u.setPasswordHash(defaultHashedPassword);
                u.setAccountLocked(true); // Force re-verification or password change
            }
        }
        userRepository.saveAll(users);
        log.info("Reset all user passwords successfully.");
    }

    // ─────────────────────────────────────────────────────
    // TRANSACTION MONITORING
    // ─────────────────────────────────────────────────────

    public Page<TransactionResponse> getAllTransactions(
            Long userId, String type, Long categoryId,
            LocalDate startDate, LocalDate endDate, Pageable pageable) {

        // FIX: parse enum safely; invalid string gets a clear 400 error
        Transaction.TransactionType txType = null;
        if (type != null && !type.isBlank()) {
            try {
                txType = Transaction.TransactionType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid transaction type: " + type + ". Must be INCOME or EXPENSE.");
            }
        }

        return transactionRepository
                .findAllWithFilters(userId, txType, categoryId, startDate, endDate, pageable)
                .map(this::toTransactionResponse);
    }

    // ─────────────────────────────────────────────────────
    // CATEGORY MANAGEMENT
    // ─────────────────────────────────────────────────────

    public List<CategoryResponse> getAllDefaultCategories() {
        return categoryRepository.findByUserIdIsNull().stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse createDefaultCategory(AdminCategoryRequest request, String adminEmail) {
        if (categoryRepository.existsByNameAndUserIdIsNull(request.getName())) {
            throw new BadRequestException("A default category with this name already exists");
        }
        Category category = Category.builder()
                .name(request.getName())
                .categoryType(Category.CategoryType.valueOf(request.getType().toUpperCase()))
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : Boolean.TRUE)
                .description(request.getDescription())
                .icon(request.getIcon())
                .color(request.getColor())
                .status(request.getStatus() != null ? 
                        Category.CategoryStatus.valueOf(request.getStatus().toUpperCase()) : 
                        Category.CategoryStatus.ACTIVE)
                .user(null)
                .build();
        categoryRepository.save(category);

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        activityLogService.log(admin, "CREATE_CATEGORY",
                "Admin created default category: " + request.getName(),
                null, ActivityLog.LogType.ADMIN_ACTION);

        return toCategoryResponse(category);
    }

    @Transactional
    public CategoryResponse updateDefaultCategory(Long categoryId, AdminCategoryRequest request, String adminEmail) {
        Category category = categoryRepository.findByIdAndUserIdIsNull(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Default category not found: " + categoryId));

        category.setName(request.getName());
        category.setCategoryType(Category.CategoryType.valueOf(request.getType().toUpperCase()));
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        if (request.getStatus() != null) {
            category.setStatus(Category.CategoryStatus.valueOf(request.getStatus().toUpperCase()));
        }
        if (request.getIsDefault() != null) category.setIsDefault(request.getIsDefault());
        categoryRepository.save(category);

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        activityLogService.log(admin, "UPDATE_CATEGORY",
                "Admin updated category #" + categoryId + " → " + request.getName(),
                null, ActivityLog.LogType.ADMIN_ACTION);

        return toCategoryResponse(category);
    }

    @Transactional
    public void deleteDefaultCategory(Long categoryId, String adminEmail) {
        Category category = categoryRepository.findByIdAndUserIdIsNull(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Default category not found: " + categoryId));

        // Business Logic: Do NOT delete if used in transactions
        long usageCount = transactionRepository.countByCategoryId(categoryId);
        if (usageCount > 0) {
            throw new BadRequestException("Category is in use by " + usageCount + " transactions. Deactivate it instead.");
        }

        String deletedName = category.getName();
        categoryRepository.delete(category);

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        activityLogService.log(admin, "DELETE_CATEGORY",
                "Admin deleted default category #" + categoryId + " (" + deletedName + ")",
                null, ActivityLog.LogType.ADMIN_ACTION);
    }

    @Transactional
    public CategoryResponse toggleDefaultCategoryStatus(Long categoryId, String adminEmail) {
        Category category = categoryRepository.findByIdAndUserIdIsNull(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Default category not found: " + categoryId));

        if (category.getStatus() == Category.CategoryStatus.ACTIVE) {
            category.setStatus(Category.CategoryStatus.INACTIVE);
        } else {
            category.setStatus(Category.CategoryStatus.ACTIVE);
        }
        categoryRepository.save(category);

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        activityLogService.log(admin, "TOGGLE_CATEGORY",
                "Admin toggled category #" + categoryId + " status to " + category.getStatus(),
                null, ActivityLog.LogType.ADMIN_ACTION);

        return toCategoryResponse(category);
    }

    // ─────────────────────────────────────────────────────
    // MAPPERS
    // ─────────────────────────────────────────────────────

    private AdminUserResponse toUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().getRoleName() : "USER")
                .status(user.getStatus() != null ? user.getStatus().name() : "ACTIVE")
                .isVerified(user.getIsVerified())
                .accountLocked(user.getAccountLocked())
                .failedAttempts(user.getFailedAttempts())
                .suspensionReason(user.getSuspensionReason())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private TransactionResponse toTransactionResponse(Transaction t) {
        // FIX: guard null category (the FK is nullable in the transactions table)
        Long   catId   = t.getCategory() != null ? t.getCategory().getId()   : null;
        String catName = t.getCategory() != null ? t.getCategory().getName() : null;

        // FIX: guard null account (shouldn't happen but avoids NPE if data is dirty)
        Long   accId   = t.getAccount() != null ? t.getAccount().getId()   : null;

        return TransactionResponse.builder()
                .id(t.getId())
                .userId(t.getUser().getId())
                .accountId(accId)
                .categoryId(catId)
                .categoryName(catName)
                .amount(t.getAmount())
                .type(t.getType())
                .description(t.getDescription())
                .transactionDate(t.getTransactionDate())
                .paymentStatus(t.getPaymentStatus())
                .source(t.getSource())
                .notes(t.getNotes())
                .receiptImageUrl(t.getReceiptImageUrl())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private CategoryResponse toCategoryResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .categoryType(c.getCategoryType())
                .isDefault(c.getIsDefault())
                .description(c.getDescription())
                .status(c.getStatus() != null ? c.getStatus().name() : "ACTIVE")
                .usedBy(transactionRepository.countDistinctUsersByCategoryId(c.getId()))
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private AdminActionResponse toActionResponse(AdminAction a) {
        return AdminActionResponse.builder()
                .id(a.getId())
                .type(a.getType().name())
                .requestedBy(a.getRequestedBy().getEmail())
                .approvedBy(a.getApprovedBy() != null ? a.getApprovedBy().getEmail() : null)
                .status(a.getStatus().name())
                .metadata(a.getMetadata())
                .reason(a.getReason())
                .createdAt(a.getCreatedAt())
                .executedAt(a.getExecutedAt())
                .build();
    }
}
