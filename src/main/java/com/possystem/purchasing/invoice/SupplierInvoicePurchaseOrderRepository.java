package com.possystem.purchasing.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierInvoicePurchaseOrderRepository extends JpaRepository<SupplierInvoicePurchaseOrder, UUID> {

    @Query("SELECT sipo FROM SupplierInvoicePurchaseOrder sipo " +
            "JOIN FETCH sipo.supplierInvoice si " +
            "WHERE sipo.purchaseOrderId = :purchaseOrderId " +
            "AND si.isActive = true " +
            "AND si.invoiceStatus != com.possystem.purchasing.enums.SupplierInvoiceStatus.CANCELLED")
    List<SupplierInvoicePurchaseOrder> findActiveByPurchaseOrderId(@Param("purchaseOrderId") UUID purchaseOrderId);

    @Query("SELECT COALESCE(SUM(sipo.allocatedAmount), 0) FROM SupplierInvoicePurchaseOrder sipo " +
            "JOIN sipo.supplierInvoice si " +
            "WHERE sipo.purchaseOrderId = :purchaseOrderId " +
            "AND si.isActive = true " +
            "AND si.invoiceStatus != com.possystem.purchasing.enums.SupplierInvoiceStatus.CANCELLED")
    BigDecimal sumAllocatedAmountByPurchaseOrderId(@Param("purchaseOrderId") UUID purchaseOrderId);
}
