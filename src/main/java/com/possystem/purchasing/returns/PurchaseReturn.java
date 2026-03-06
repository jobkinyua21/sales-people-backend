package com.possystem.purchasing.returns;

import com.possystem.audit.Auditable;
import com.possystem.purchasing.enums.PurchaseReturnStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "purchase_return", schema = "pos_core", indexes = {
        @Index(name = "idx_pr_shop_active", columnList = "shop_id, is_active"),
        @Index(name = "idx_pr_grn", columnList = "grn_id"),
        @Index(name = "idx_pr_po", columnList = "purchase_order_id"),
        @Index(name = "idx_pr_supplier", columnList = "supplier_id"),
        @Index(name = "idx_pr_status", columnList = "return_status"),
        @Index(name = "idx_pr_number", columnList = "shop_id, return_number"),
        @Index(name = "idx_pr_created_at", columnList = "created_at DESC")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class PurchaseReturn extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(name = "return_number", nullable = false, length = 50)
    private String returnNumber;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "grn_id", nullable = false)
    private UUID grnId;

    @Column(name = "purchase_order_id")
    private UUID purchaseOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_status", nullable = false, length = 20)
    @Builder.Default
    private PurchaseReturnStatus returnStatus = PurchaseReturnStatus.DRAFT;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(name = "returned_by")
    private UUID returnedBy;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "stock_deducted", nullable = false)
    @Builder.Default
    private Boolean stockDeducted = false;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "purchaseReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseReturnItem> items = new ArrayList<>();
}
