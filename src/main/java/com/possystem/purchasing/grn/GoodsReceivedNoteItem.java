package com.possystem.purchasing.grn;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "goods_received_note_item", schema = "pos_core", indexes = {
        @Index(name = "idx_grn_item_grn", columnList = "grn_id"),
        @Index(name = "idx_grn_item_po_item", columnList = "purchase_order_item_id"),
        @Index(name = "idx_grn_item_variant", columnList = "variant_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class GoodsReceivedNoteItem extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    private GoodsReceivedNote goodsReceivedNote;

    @Column(name = "purchase_order_item_id", nullable = false)
    private UUID purchaseOrderItemId;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "variant_name", nullable = false, length = 200)
    private String variantName;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "quantity_received", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityReceived;

    @Column(name = "quantity_damaged", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal quantityDamaged = BigDecimal.ZERO;

    @Column(name = "quantity_missing", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal quantityMissing = BigDecimal.ZERO;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "returned_quantity", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal returnedQuantity = BigDecimal.ZERO;

    @Column(name = "notes", length = 500)
    private String notes;
}
