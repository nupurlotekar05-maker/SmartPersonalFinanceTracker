package com.finance.tracker.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // null = global/default category

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type")
    private CategoryType categoryType;

    @Builder.Default
    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Builder.Default
    @Column(name = "created_by_admin")
    private Boolean createdByAdmin = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String icon;

    @Column(length = 20)
    private String color;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CategoryStatus status = CategoryStatus.ACTIVE;

    public enum CategoryType {
        INCOME, EXPENSE, BOTH
    }

    public enum CategoryStatus {
        ACTIVE, INACTIVE
    }
}
