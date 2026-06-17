package com.finance.tracker.service;

import com.finance.tracker.dto.response.ActivityLogResponse;
import com.finance.tracker.entity.ActivityLog;
import com.finance.tracker.entity.User;
import com.finance.tracker.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Async
    public void log(User user, String action, String description, String ipAddress, ActivityLog.LogType logType) {
        ActivityLog log = ActivityLog.builder()
                .user(user)
                .action(action)
                .description(description)
                .ipAddress(ipAddress)
                .logType(logType)
                .build();
        activityLogRepository.save(log);
    }

    @Async
    public void logAnonymous(String action, String description, String ipAddress, ActivityLog.LogType logType) {
        ActivityLog log = ActivityLog.builder()
                .user(null)
                .action(action)
                .description(description)
                .ipAddress(ipAddress)
                .logType(logType)
                .build();
        activityLogRepository.save(log);
    }

    public Page<ActivityLogResponse> getAllLogs(Pageable pageable) {
        return activityLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    public Page<ActivityLogResponse> getLoginLogs(Pageable pageable) {
        return activityLogRepository.findByLogTypeIn(
                List.of(ActivityLog.LogType.LOGIN_SUCCESS, ActivityLog.LogType.LOGIN_FAILED,
                        ActivityLog.LogType.LOGIN_BLOCKED, ActivityLog.LogType.LOGOUT),
                pageable
        ).map(this::toResponse);
    }

    public Page<ActivityLogResponse> getFailedLoginLogs(Pageable pageable) {
        return activityLogRepository.findByLogTypeIn(
                List.of(ActivityLog.LogType.LOGIN_FAILED, ActivityLog.LogType.LOGIN_BLOCKED),
                pageable
        ).map(this::toResponse);
    }

    public Page<ActivityLogResponse> getBlockedLoginLogs(Pageable pageable) {
        return activityLogRepository.findByLogTypeOrderByCreatedAtDesc(ActivityLog.LogType.LOGIN_BLOCKED, pageable)
                .map(this::toResponse);
    }

    public Page<ActivityLogResponse> getAdminActionLogs(Pageable pageable) {
        return activityLogRepository.findByLogTypeOrderByCreatedAtDesc(ActivityLog.LogType.ADMIN_ACTION, pageable)
                .map(this::toResponse);
    }

    public Page<ActivityLogResponse> getSecurityAlerts(Pageable pageable) {
        return activityLogRepository.findByLogTypeOrderByCreatedAtDesc(ActivityLog.LogType.SECURITY_ALERT, pageable)
                .map(this::toResponse);
    }

    public List<ActivityLogResponse> getUserActivityLogs(Long userId) {
        return activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public long countRecentFailedLogins() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return activityLogRepository.countFailedLoginsAfter(oneHourAgo);
    }

    private ActivityLogResponse toResponse(ActivityLog log) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .userName(log.getUser() != null ? log.getUser().getName() : "Anonymous")
                .userEmail(log.getUser() != null ? log.getUser().getEmail() : null)
                .action(log.getAction())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .logType(log.getLogType().name())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
