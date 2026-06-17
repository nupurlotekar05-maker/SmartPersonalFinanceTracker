package com.finance.tracker.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_bills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "bill_name", length = 150)
    private String billName;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    public enum Frequency {
        DAILY, WEEKLY, MONTHLY, YEARLY
    }
}
