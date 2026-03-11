package com.possystem.kitchen.kot;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "kitchen_order_ticket", schema = "pos_core", indexes = {
        @Index(name = "idx_kot_shop", columnList = "shop_id"),
        @Index(name = "idx_kot_order", columnList = "order_id"),
        @Index(name = "idx_kot_status", columnList = "status"),
@Index(name = "idx_kot_created_at", columnList = "created_at DESC")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class KitchenOrderTicket extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(name = "kot_number", nullable = false, length = 20)
    private String kotNumber;

    // Linked sales order
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "order_number", length = 50)
    private String orderNumber;

    // Table number from the sales order (optional — takeaway orders have no table)
    @Column(name = "table_number", length = 20)
    private String tableNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private KotStatus status = KotStatus.PENDING;

    // Priority (1 = highest, used for rush orders)
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 5;

    // Special instructions for the entire KOT
    @Column(name = "special_instructions", length = 1000)
    private String specialInstructions;

    // Who sent the KOT (waiter)
    @Column(name = "sent_by", nullable = false)
    private UUID sentBy;

    // Kitchen staff who picked up the ticket
    @Column(name = "accepted_by")
    private UUID acceptedBy;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    // Whether ingredients have been deducted for this KOT
    @Column(name = "ingredients_deducted", nullable = false)
    @Builder.Default
    private Boolean ingredientsDeducted = false;

    @OneToMany(mappedBy = "kitchenOrderTicket", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<KitchenOrderTicketItem> items = new ArrayList<>();
}
