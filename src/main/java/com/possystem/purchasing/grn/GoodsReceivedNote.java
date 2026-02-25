package com.possystem.purchasing.grn;

import com.possystem.audit.Auditable;
import com.possystem.purchasing.enums.GrnStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "goods_received_note", schema = "pos_core", indexes = {
        @Index(name = "idx_grn_shop_active", columnList = "shop_id, is_active"),
        @Index(name = "idx_grn_po", columnList = "purchase_order_id"),
        @Index(name = "idx_grn_status", columnList = "grn_status"),
        @Index(name = "idx_grn_number", columnList = "shop_id, grn_number"),
        @Index(name = "idx_grn_created_at", columnList = "created_at DESC")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class GoodsReceivedNote extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(name = "grn_number", nullable = false, length = 50)
    private String grnNumber;

    @Column(name = "purchase_order_id", nullable = false)
    private UUID purchaseOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "grn_status", nullable = false, length = 20)
    @Builder.Default
    private GrnStatus grnStatus = GrnStatus.PENDING;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column(name = "received_by")
    private UUID receivedBy;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "stock_updated", nullable = false)
    @Builder.Default
    private Boolean stockUpdated = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "goodsReceivedNote", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GoodsReceivedNoteItem> items = new ArrayList<>();
}
