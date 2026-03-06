package com.possystem.expense.category;

import com.possystem.audit.Auditable;
import com.possystem.expense.enums.ExpenseCategoryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "expense_category", schema = "pos_core", indexes = {
        @Index(name = "idx_expense_cat_shop_active", columnList = "shop_id, is_active"),
        @Index(name = "idx_expense_cat_code", columnList = "shop_id, category_code"),
        @Index(name = "idx_expense_cat_name", columnList = "shop_id, category_name"),
        @Index(name = "idx_expense_cat_status", columnList = "status"),
        @Index(name = "idx_expense_cat_created_at", columnList = "created_at DESC")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ExpenseCategory extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(name = "category_code", nullable = false, length = 50)
    private String categoryCode;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ExpenseCategoryStatus status = ExpenseCategoryStatus.ACTIVE;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
