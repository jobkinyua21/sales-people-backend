package com.possystem.mpesa;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mpesa_transaction", schema = "pos_core", indexes = {
        @Index(name = "idx_mpesa_tx_order", columnList = "order_id"),
        @Index(name = "idx_mpesa_tx_checkout", columnList = "checkout_request_id"),
        @Index(name = "idx_mpesa_tx_shop", columnList = "shop_id"),
        @Index(name = "idx_mpesa_tx_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class MpesaTransaction extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "merchant_request_id", length = 100)
    private String merchantRequestId;

    @Column(name = "checkout_request_id", length = 100)
    private String checkoutRequestId;

    @Column(name = "mpesa_receipt_number", length = 50)
    private String mpesaReceiptNumber;

    @Column(name = "result_code")
    private Integer resultCode;

    @Column(name = "result_description", length = 500)
    private String resultDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MpesaTransactionStatus status = MpesaTransactionStatus.PENDING;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
