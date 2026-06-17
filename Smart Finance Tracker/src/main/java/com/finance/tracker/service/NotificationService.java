package com.finance.tracker.service;

import com.finance.tracker.dto.response.NotificationResponse;
import com.finance.tracker.entity.*;
import com.finance.tracker.repository.NotificationRepository;
import com.finance.tracker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SecurityUtils securityUtils;

    public List<NotificationResponse> getAll() {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(securityUtils.getCurrentUserId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<NotificationResponse> getUnread() {
        return notificationRepository
                .findByUserIdAndIsReadFalse(securityUtils.getCurrentUserId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public long getUnreadCount() {
        return notificationRepository.countByUserIdAndIsReadFalse(securityUtils.getCurrentUserId());
    }

    @Transactional
    public void markAllRead() {
        notificationRepository.markAllReadByUserId(securityUtils.getCurrentUserId());
    }

    @Transactional
    public void markRead(Long id) {
        Notification notification = notificationRepository.findByIdAndUserId(id, securityUtils.getCurrentUserId())
                .orElseThrow(() -> new com.finance.tracker.exception.ResourceNotFoundException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void deleteNotification(Long id) {
        Notification notification = notificationRepository.findByIdAndUserId(id, securityUtils.getCurrentUserId())
                .orElseThrow(() -> new com.finance.tracker.exception.ResourceNotFoundException("Notification not found"));
        notificationRepository.delete(notification);
    }

    // Called internally by other services
    public void createBudgetNotification(User user, String message) {
        save(user, "Budget Alert", message, Notification.NotificationType.BUDGET);
    }

    public void createBillNotification(User user, String message) {
        save(user, "Bill Reminder", message, Notification.NotificationType.BILL);
    }

    public void createSystemNotification(User user, String message) {
        save(user, "System Notice", message, Notification.NotificationType.SYSTEM);
    }

    private void save(User user, String title, String message, Notification.NotificationType type) {
        Notification n = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .build();
        notificationRepository.save(n);
        // Real-time push removed for stability
    }

    public void broadcast(String title, String message, Notification.NotificationType type) {
        // Broadcast via WebSocket removed for stability. 
        // Notifications are now pull-based via REST API.
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
