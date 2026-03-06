package com.possystem.purchasing.returns;

import com.possystem.audit.Auditable;
import com.possystem.purchasing.enums.ReturnReason;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "purchase_return_item", schema = "pos_core", indexes = {
        @Index(name = "idx_pr_item_return", columnList = "purchase_return_id"),
        @Index(name = "idx_pr_item_variant", columnList = "variant_id"),
        @Index(name = "idx_pr_item_grn_item", columnList = "grn_item_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class PurchaseReturnItem extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_return_id", nullable = false)
    private PurchaseReturn purchaseReturn;

    @Column(name = "grn_item_id", nullable = false)
    private UUID grnItemId;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "variant_name", nullable = false, length = 200)
    private String variantName;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "quantity_returned", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityReturned;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_reason", length = 20)
    private ReturnReason returnReason;

    @Column(name = "notes", length = 500)
    private String notes;
}
