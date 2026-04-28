package com.salespeople.batchorder;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sale_person_batch_order", schema = "public")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SalesPersonBatchOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_order_id")
    @EqualsAndHashCode.Include
    private Long batchOrderId;

    // Groups multiple orders submitted together by the same salesperson
    @Column(name = "batch_ref", nullable = false, length = 100)
    private String batchRef;

    @Column(name = "sales_order_header_id", nullable = false)
    private Long salesOrderHeaderId;

    @Column(name = "sales_person_number", nullable = false)
    private Integer salesPersonNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SalesPersonBatchOrderStatus status = SalesPersonBatchOrderStatus.PENDING;

    @Column(name = "order_date")
    private LocalDate orderDate;

    // Manager who reviewed this order
    @Column(name = "reviewed_by", length = 200)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_by", length = 200)
    private String createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
