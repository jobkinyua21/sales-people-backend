package com.possystem.expense;

import com.possystem.audit.Auditable;
import com.possystem.expense.enums.ExpenseStatus;
import com.possystem.sales.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "expense", schema = "pos_core", indexes = {
        @Index(name = "idx_expense_shop_active", columnList = "shop_id, is_active"),
        @Index(name = "idx_expense_number", columnList = "shop_id, expense_number"),
        @Index(name = "idx_expense_category", columnList = "expense_category_id"),
        @Index(name = "idx_expense_status", columnList = "expense_status"),
        @Index(name = "idx_expense_date", columnList = "expense_date"),
        @Index(name = "idx_expense_payment_method", columnList = "payment_method"),
        @Index(name = "idx_expense_created_at", columnList = "created_at DESC")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Expense extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(name = "expense_number", nullable = false, length = 50)
    private String expenseNumber;

    @Column(name = "expense_category_id", nullable = false)
    private UUID expenseCategoryId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_status", nullable = false, length = 20)
    @Builder.Default
    private ExpenseStatus expenseStatus = ExpenseStatus.PENDING;

    @Column(name = "vendor", length = 200)
    private String vendor;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_by")
    private UUID rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
