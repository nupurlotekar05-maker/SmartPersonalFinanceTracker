package com.finance.tracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_actions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON representation of action details

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ActionStatus status = ActionStatus.PENDING;

    private String reason;

    private LocalDateTime executedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum ActionType {
        RESET_ALL_PASSWORDS,
        CLEAR_LOGS,
        EXPORT_DATA,
        BROADCAST_NOTIFICATION,
        SYSTEM_CLEANUP
    }

    public enum ActionStatus {
        PENDING, APPROVED, REJECTED, EXECUTED
    }
}
