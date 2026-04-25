package com.salespeople.discount;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "discount_setup", schema = "public")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DiscountSetup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_setup_id")
    @EqualsAndHashCode.Include
    private Long discountSetupId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // unique per item_code
    @Column(name = "item_code", nullable = false, unique = true)
    private Integer itemCode;

    @Column(name = "discount_start_value", precision = 15, scale = 2)
    private BigDecimal discountStartValue;

    @Column(name = "discount_value", precision = 15, scale = 2)
    private BigDecimal discountValue;
}
