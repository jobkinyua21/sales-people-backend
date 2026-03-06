package com.possystem.sales.register;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cash_register_session", schema = "pos_core", indexes = {
        @Index(name = "idx_register_session_shop_active", columnList = "shop_id, status"),
        @Index(name = "idx_register_session_opened_by", columnList = "opened_by")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class CashRegisterSession extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(name = "opened_by", nullable = false)
    private UUID openedBy;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "opening_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "expected_closing_balance", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal expectedClosingBalance = BigDecimal.ZERO;

    @Column(name = "actual_closing_balance", precision = 12, scale = 2)
    private BigDecimal actualClosingBalance;

    @Column(name = "difference", precision = 12, scale = 2)
    private BigDecimal difference;

    @Column(name = "total_cash_sales", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCashSales = BigDecimal.ZERO;

    @Column(name = "total_cash_in", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCashIn = BigDecimal.ZERO;

    @Column(name = "total_cash_out", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCashOut = BigDecimal.ZERO;

    @Column(name = "total_cash_refunds", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCashRefunds = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RegisterSessionStatus status = RegisterSessionStatus.OPEN;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CashMovement> movements = new ArrayList<>();
}
