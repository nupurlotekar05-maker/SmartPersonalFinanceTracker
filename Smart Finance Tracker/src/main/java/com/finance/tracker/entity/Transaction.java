package com.finance.tracker.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(length = 255)
    private String description;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private TransactionSource source = TransactionSource.MANUAL;

    @Column(name = "receipt_image_url", length = 255)
    private String receiptImageUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp                         
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
     
    public enum TransactionType {
        INCOME, EXPENSE
    }

    public enum PaymentStatus {
        PENDING, PAID
    }

    public enum TransactionSource {
        MANUAL, VOICE, RECEIPT, PAYMENT
    }
}
