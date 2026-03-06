package com.possystem.purchasing.payment;

import com.possystem.audit.Auditable;
import com.possystem.purchasing.invoice.SupplierInvoice;
import com.possystem.sales.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "purchase_payment", schema = "pos_core", indexes = {
        @Index(name = "idx_purchase_payment_invoice", columnList = "supplier_invoice_id"),
        @Index(name = "idx_purchase_payment_method", columnList = "payment_method"),
        @Index(name = "idx_purchase_payment_voucher", columnList = "voucher_number")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class PurchasePayment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "voucher_number", nullable = false, unique = true, length = 50)
    private String voucherNumber;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_invoice_id", nullable = false)
    private SupplierInvoice supplierInvoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "paid_by")
    private UUID paidBy;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;
}
