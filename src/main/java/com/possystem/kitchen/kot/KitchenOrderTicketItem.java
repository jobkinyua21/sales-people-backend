package com.possystem.kitchen.kot;

import com.possystem.audit.Auditable;
import com.possystem.kitchen.recipe.PrepStation;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kitchen_order_ticket_item", schema = "pos_core", indexes = {
        @Index(name = "idx_kot_item_kot", columnList = "kitchen_order_ticket_id"),
        @Index(name = "idx_kot_item_status", columnList = "status"),
        @Index(name = "idx_kot_item_station", columnList = "prep_station")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class KitchenOrderTicketItem extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kitchen_order_ticket_id", nullable = false)
    private KitchenOrderTicket kitchenOrderTicket;

    // Reference to the sales order item
    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    // The product variant being prepared
    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    // Snapshot fields
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "variant_name", length = 200)
    private String variantName;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private KotItemStatus status = KotItemStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "prep_station", length = 30)
    private PrepStation prepStation;

    // Item-level special instructions (e.g., "no onions", "extra spicy")
    @Column(name = "special_instructions", length = 500)
    private String specialInstructions;

    // Linked recipe for ingredient deduction
    @Column(name = "recipe_id")
    private UUID recipeId;

    // Who is preparing this item
    @Column(name = "prepared_by")
    private UUID preparedBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    @Column(name = "served_at")
    private LocalDateTime servedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;
}
