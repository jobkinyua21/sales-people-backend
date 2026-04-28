package com.salespeople.salesorder;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sales_order_headers", schema = "public")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SalesOrderHeader {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_order_header_id")
    @EqualsAndHashCode.Include
    private Long salesOrderHeaderId;

    // DB auto-generates via sequence — set insertable=false so Hibernate does not touch it
    @Column(name = "sales_order_number", unique = true, nullable = false, insertable = false, updatable = false)
    private Long salesOrderNumber;

    @Column(name = "sale_order_type", nullable = false, length = 200)
    private String saleOrderType;

    @Column(name = "sales_person_number")
    private Integer salesPersonNumber;

    @Convert(converter = SalesOrderStatusConverter.class)
    @Column(name = "status", length = 200)
    @Builder.Default
    private SalesOrderStatus status = SalesOrderStatus.NEW;

    @Column(name = "sales_order_date", nullable = false)
    private LocalDate salesOrderDate;

    @Column(name = "entry_date", insertable = false, updatable = false)
    private OffsetDateTime entryDate;

    @Column(name = "sales_order_total_value", precision = 15, scale = 2)
    private BigDecimal salesOrderTotalValue;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "phone_number", length = 200)
    private String phoneNumber;

    @Column(name = "created_by", length = 200)
    private String createdBy;

    @Column(name = "total", precision = 15, scale = 2)
    private BigDecimal total;

    @Column(name = "number_of_items")
    private Integer numberOfItems;

    @Column(name = "batch_number", length = 200)
    private String batchNumber;

    @Column(name = "institution_id", length = 200)
    @Builder.Default
    private String institutionId = "null";

    @Column(name = "customer_id", length = 200)
    private String customerId;

    @Column(name = "discount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;
}
