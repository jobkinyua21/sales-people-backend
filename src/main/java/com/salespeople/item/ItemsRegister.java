package com.salespeople.item;

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
@Table(name = "items_register", schema = "public")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ItemsRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_register_id")
    @EqualsAndHashCode.Include
    private Long itemRegisterId;

    @Column(name = "item_code", unique = true, nullable = false)
    private Integer itemCode;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "item_units", nullable = false, length = 200)
    private String itemUnits;

    @Column(name = "item_units_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal itemUnitsValue;

    @Column(name = "item_units_abbreaviations", length = 200)
    private String itemUnitsAbbreviations;

    @Column(name = "prevoius_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal previousPrice;

    @Column(name = "current_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "account_number", nullable = false)
    private Integer accountNumber;

    @Column(name = "created_by", nullable = false, length = 200)
    private String createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    // "contraint" is the exact DB column name (typo in original schema — kept as-is)
    @Column(name = "contraint", precision = 15, scale = 2)
    private BigDecimal constraint;

    @Column(name = "status")
    @Builder.Default
    private Boolean status = false;

    @Column(name = "diabled")
    @Builder.Default
    private Boolean disabled = false;
}
