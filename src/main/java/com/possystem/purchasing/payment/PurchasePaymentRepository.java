package com.possystem.purchasing.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurchasePaymentRepository extends JpaRepository<PurchasePayment, UUID> {

    List<PurchasePayment> findBySupplierInvoiceId(UUID supplierInvoiceId);

    long countByShopId(UUID shopId);

    boolean existsByShopIdAndVoucherNumber(UUID shopId, String voucherNumber);
}
