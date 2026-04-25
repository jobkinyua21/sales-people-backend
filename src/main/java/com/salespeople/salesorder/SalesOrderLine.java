package com.salespeople.salesorder;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sales_orders_lines", schema = "public")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SalesOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_line_id")
    @EqualsAndHashCode.Include
    private Long orderLineId;

    @Column(name = "sales_order_number", nullable = false)
    private Long salesOrderNumber;

    @Column(name = "item_code", nullable = false)
    private Integer itemCode;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "account_number", nullable = false)
    private Integer accountNumber;

    @Column(name = "account_name", length = 200)
    private String accountName;

    @Column(name = "store_code", nullable = false)
    private Integer storeCode;

    @Column(name = "store_name", length = 200)
    private String storeName;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "cost_per_item", precision = 15, scale = 0)
    private BigDecimal costPerItem;

    @Column(name = "vat", precision = 15, scale = 2)
    private BigDecimal vat;

    @Column(name = "sub_total", precision = 15, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "total", precision = 15, scale = 2)
    private BigDecimal total;

    @Column(name = "status", length = 200)
    @Builder.Default
    private String status = "New";

    @Column(name = "created_by", length = 200)
    private String createdBy;

    @Column(name = "sales_person_number")
    private Integer salesPersonNumber;

    @Column(name = "entry_date", insertable = false, updatable = false)
    private OffsetDateTime entryDate;

    @Column(name = "discount_value", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(name = "discount_start_value", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountStartValue = BigDecimal.ZERO;
}
