package com.possystem.purchasing.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    long countByShopId(UUID shopId);

    Optional<PurchaseOrder> findByIdAndShopIdAndIsActiveTrue(UUID id, UUID shopId);

    List<PurchaseOrder> findAllByIdInAndShopIdAndIsActiveTrue(List<UUID> ids, UUID shopId);

    @Query(value = "SELECT po.* FROM pos_core.purchase_order po " +
            "LEFT JOIN pos_core.supplier s ON s.id = po.supplier_id AND s.is_active = true " +
            "WHERE po.shop_id = :shopId AND po.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(po.po_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(s.supplier_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(po.reference_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:orderStatus AS text) IS NULL OR po.order_status = CAST(:orderStatus AS text)) AND " +
            "(CAST(:paymentStatus AS text) IS NULL OR po.payment_status = CAST(:paymentStatus AS text)) AND " +
            "(CAST(:supplierId AS uuid) IS NULL OR po.supplier_id = CAST(:supplierId AS uuid)) AND " +
            "(CAST(:dateFrom AS timestamp) IS NULL OR po.created_at >= CAST(:dateFrom AS timestamp)) AND " +
            "(CAST(:dateTo AS timestamp) IS NULL OR po.created_at <= CAST(:dateTo AS timestamp)) " +
            "ORDER BY po.created_at DESC",
            countQuery = "SELECT COUNT(po.id) FROM pos_core.purchase_order po " +
                    "LEFT JOIN pos_core.supplier s ON s.id = po.supplier_id AND s.is_active = true " +
                    "WHERE po.shop_id = :shopId AND po.is_active = true AND " +
                    "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
                    "LOWER(po.po_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
                    "LOWER(s.supplier_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
                    "LOWER(po.reference_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
                    "(CAST(:orderStatus AS text) IS NULL OR po.order_status = CAST(:orderStatus AS text)) AND " +
                    "(CAST(:paymentStatus AS text) IS NULL OR po.payment_status = CAST(:paymentStatus AS text)) AND " +
                    "(CAST(:supplierId AS uuid) IS NULL OR po.supplier_id = CAST(:supplierId AS uuid)) AND " +
                    "(CAST(:dateFrom AS timestamp) IS NULL OR po.created_at >= CAST(:dateFrom AS timestamp)) AND " +
                    "(CAST(:dateTo AS timestamp) IS NULL OR po.created_at <= CAST(:dateTo AS timestamp))",
            nativeQuery = true)
    Page<PurchaseOrder> searchFiltered(@Param("shopId") UUID shopId,
                                       @Param("search") String search,
                                       @Param("orderStatus") String orderStatus,
                                       @Param("paymentStatus") String paymentStatus,
                                       @Param("supplierId") UUID supplierId,
                                       @Param("dateFrom") LocalDateTime dateFrom,
                                       @Param("dateTo") LocalDateTime dateTo,
                                       Pageable pageable);

    @Query(value = "SELECT po.* FROM pos_core.purchase_order po " +
            "LEFT JOIN pos_core.supplier s ON s.id = po.supplier_id AND s.is_active = true " +
            "WHERE po.shop_id = :shopId AND po.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(po.po_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(s.supplier_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(po.reference_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:orderStatus AS text) IS NULL OR po.order_status = CAST(:orderStatus AS text)) AND " +
            "(CAST(:paymentStatus AS text) IS NULL OR po.payment_status = CAST(:paymentStatus AS text)) AND " +
            "(CAST(:supplierId AS uuid) IS NULL OR po.supplier_id = CAST(:supplierId AS uuid)) AND " +
            "(CAST(:dateFrom AS timestamp) IS NULL OR po.created_at >= CAST(:dateFrom AS timestamp)) AND " +
            "(CAST(:dateTo AS timestamp) IS NULL OR po.created_at <= CAST(:dateTo AS timestamp)) " +
            "ORDER BY po.created_at DESC",
            nativeQuery = true)
    List<PurchaseOrder> searchFilteredUnpaged(@Param("shopId") UUID shopId,
                                              @Param("search") String search,
                                              @Param("orderStatus") String orderStatus,
                                              @Param("paymentStatus") String paymentStatus,
                                              @Param("supplierId") UUID supplierId,
                                              @Param("dateFrom") LocalDateTime dateFrom,
                                              @Param("dateTo") LocalDateTime dateTo);
}
