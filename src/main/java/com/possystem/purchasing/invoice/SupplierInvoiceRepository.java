package com.possystem.purchasing.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, UUID> {

    long countByShopId(UUID shopId);

    Optional<SupplierInvoice> findByIdAndShopIdAndIsActiveTrue(UUID id, UUID shopId);

    List<SupplierInvoice> findAllByIdInAndShopIdAndIsActiveTrue(List<UUID> ids, UUID shopId);

    @Query(value = "SELECT si.* FROM pos_core.supplier_invoice si " +
            "LEFT JOIN pos_core.supplier s ON s.id = si.supplier_id AND s.is_active = true " +
            "WHERE si.shop_id = :shopId AND si.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(si.invoice_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(si.reference_code) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(s.supplier_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:invoiceStatus AS text) IS NULL OR si.invoice_status = CAST(:invoiceStatus AS text)) AND " +
            "(CAST(:paymentStatus AS text) IS NULL OR si.payment_status = CAST(:paymentStatus AS text)) AND " +
            "(CAST(:supplierId AS uuid) IS NULL OR si.supplier_id = CAST(:supplierId AS uuid)) AND " +
            "(CAST(:dueDateFrom AS date) IS NULL OR si.due_date >= CAST(:dueDateFrom AS date)) AND " +
            "(CAST(:dueDateTo AS date) IS NULL OR si.due_date <= CAST(:dueDateTo AS date)) AND " +
            "(CAST(:dateFrom AS timestamp) IS NULL OR si.created_at >= CAST(:dateFrom AS timestamp)) AND " +
            "(CAST(:dateTo AS timestamp) IS NULL OR si.created_at <= CAST(:dateTo AS timestamp)) AND " +
            "(:overdueOnly = false OR (si.due_date < CURRENT_DATE AND si.payment_status != 'PAID')) " +
            "ORDER BY si.created_at DESC",
            countQuery = "SELECT COUNT(si.id) FROM pos_core.supplier_invoice si " +
                    "LEFT JOIN pos_core.supplier s ON s.id = si.supplier_id AND s.is_active = true " +
                    "WHERE si.shop_id = :shopId AND si.is_active = true AND " +
                    "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
                    "LOWER(si.invoice_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
                    "LOWER(si.reference_code) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
                    "LOWER(s.supplier_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
                    "(CAST(:invoiceStatus AS text) IS NULL OR si.invoice_status = CAST(:invoiceStatus AS text)) AND " +
                    "(CAST(:paymentStatus AS text) IS NULL OR si.payment_status = CAST(:paymentStatus AS text)) AND " +
                    "(CAST(:supplierId AS uuid) IS NULL OR si.supplier_id = CAST(:supplierId AS uuid)) AND " +
                    "(CAST(:dueDateFrom AS date) IS NULL OR si.due_date >= CAST(:dueDateFrom AS date)) AND " +
                    "(CAST(:dueDateTo AS date) IS NULL OR si.due_date <= CAST(:dueDateTo AS date)) AND " +
                    "(CAST(:dateFrom AS timestamp) IS NULL OR si.created_at >= CAST(:dateFrom AS timestamp)) AND " +
                    "(CAST(:dateTo AS timestamp) IS NULL OR si.created_at <= CAST(:dateTo AS timestamp)) AND " +
                    "(:overdueOnly = false OR (si.due_date < CURRENT_DATE AND si.payment_status != 'PAID'))",
            nativeQuery = true)
    Page<SupplierInvoice> searchFiltered(@Param("shopId") UUID shopId,
                                          @Param("search") String search,
                                          @Param("invoiceStatus") String invoiceStatus,
                                          @Param("paymentStatus") String paymentStatus,
                                          @Param("supplierId") UUID supplierId,
                                          @Param("dueDateFrom") LocalDate dueDateFrom,
                                          @Param("dueDateTo") LocalDate dueDateTo,
                                          @Param("dateFrom") LocalDateTime dateFrom,
                                          @Param("dateTo") LocalDateTime dateTo,
                                          @Param("overdueOnly") boolean overdueOnly,
                                          Pageable pageable);

    @Query(value = "SELECT si.* FROM pos_core.supplier_invoice si " +
            "LEFT JOIN pos_core.supplier s ON s.id = si.supplier_id AND s.is_active = true " +
            "WHERE si.shop_id = :shopId AND si.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(si.invoice_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(si.reference_code) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(s.supplier_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:invoiceStatus AS text) IS NULL OR si.invoice_status = CAST(:invoiceStatus AS text)) AND " +
            "(CAST(:paymentStatus AS text) IS NULL OR si.payment_status = CAST(:paymentStatus AS text)) AND " +
            "(CAST(:supplierId AS uuid) IS NULL OR si.supplier_id = CAST(:supplierId AS uuid)) AND " +
            "(CAST(:dueDateFrom AS date) IS NULL OR si.due_date >= CAST(:dueDateFrom AS date)) AND " +
            "(CAST(:dueDateTo AS date) IS NULL OR si.due_date <= CAST(:dueDateTo AS date)) AND " +
            "(CAST(:dateFrom AS timestamp) IS NULL OR si.created_at >= CAST(:dateFrom AS timestamp)) AND " +
            "(CAST(:dateTo AS timestamp) IS NULL OR si.created_at <= CAST(:dateTo AS timestamp)) AND " +
            "(:overdueOnly = false OR (si.due_date < CURRENT_DATE AND si.payment_status != 'PAID')) " +
            "ORDER BY si.created_at DESC",
            nativeQuery = true)
    List<SupplierInvoice> searchFilteredUnpaged(@Param("shopId") UUID shopId,
                                                 @Param("search") String search,
                                                 @Param("invoiceStatus") String invoiceStatus,
                                                 @Param("paymentStatus") String paymentStatus,
                                                 @Param("supplierId") UUID supplierId,
                                                 @Param("dueDateFrom") LocalDate dueDateFrom,
                                                 @Param("dueDateTo") LocalDate dueDateTo,
                                                 @Param("dateFrom") LocalDateTime dateFrom,
                                                 @Param("dateTo") LocalDateTime dateTo,
                                                 @Param("overdueOnly") boolean overdueOnly);
}
