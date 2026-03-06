package com.possystem.purchasing.grn;

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
public interface GoodsReceivedNoteRepository extends JpaRepository<GoodsReceivedNote, UUID> {

    long countByShopId(UUID shopId);

    Optional<GoodsReceivedNote> findByIdAndShopIdAndIsActiveTrue(UUID id, UUID shopId);

    List<GoodsReceivedNote> findByPurchaseOrderIdAndIsActiveTrue(UUID purchaseOrderId);

    @Query(value = "SELECT g.* FROM pos_core.goods_received_note g " +
            "LEFT JOIN pos_core.purchase_order po ON po.id = g.purchase_order_id AND po.is_active = true " +
            "LEFT JOIN pos_core.supplier s ON s.id = po.supplier_id AND s.is_active = true " +
            "WHERE g.shop_id = :shopId AND g.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(g.grn_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(po.po_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(s.supplier_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(g.reference_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:grnStatus AS text) IS NULL OR g.grn_status = CAST(:grnStatus AS text)) AND " +
            "(CAST(:supplierId AS uuid) IS NULL OR po.supplier_id = CAST(:supplierId AS uuid)) AND " +
            "(CAST(:purchaseOrderId AS uuid) IS NULL OR g.purchase_order_id = CAST(:purchaseOrderId AS uuid)) AND " +
            "(CAST(:dateFrom AS timestamp) IS NULL OR g.created_at >= CAST(:dateFrom AS timestamp)) AND " +
            "(CAST(:dateTo AS timestamp) IS NULL OR g.created_at <= CAST(:dateTo AS timestamp)) " +
            "ORDER BY g.created_at DESC",
            countQuery = "SELECT COUNT(g.id) FROM pos_core.goods_received_note g " +
                    "LEFT JOIN pos_core.purchase_order po ON po.id = g.purchase_order_id AND po.is_active = true " +
                    "LEFT JOIN pos_core.supplier s ON s.id = po.supplier_id AND s.is_active = true " +
                    "WHERE g.shop_id = :shopId AND g.is_active = true AND " +
                    "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
                    "LOWER(g.grn_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
                    "LOWER(po.po_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
                    "LOWER(s.supplier_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
                    "LOWER(g.reference_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
                    "(CAST(:grnStatus AS text) IS NULL OR g.grn_status = CAST(:grnStatus AS text)) AND " +
                    "(CAST(:supplierId AS uuid) IS NULL OR po.supplier_id = CAST(:supplierId AS uuid)) AND " +
                    "(CAST(:purchaseOrderId AS uuid) IS NULL OR g.purchase_order_id = CAST(:purchaseOrderId AS uuid)) AND " +
                    "(CAST(:dateFrom AS timestamp) IS NULL OR g.created_at >= CAST(:dateFrom AS timestamp)) AND " +
                    "(CAST(:dateTo AS timestamp) IS NULL OR g.created_at <= CAST(:dateTo AS timestamp))",
            nativeQuery = true)
    Page<GoodsReceivedNote> searchFiltered(@Param("shopId") UUID shopId,
                                            @Param("search") String search,
                                            @Param("grnStatus") String grnStatus,
                                            @Param("supplierId") UUID supplierId,
                                            @Param("purchaseOrderId") UUID purchaseOrderId,
                                            @Param("dateFrom") LocalDateTime dateFrom,
                                            @Param("dateTo") LocalDateTime dateTo,
                                            Pageable pageable);

    @Query(value = "SELECT g.* FROM pos_core.goods_received_note g " +
            "LEFT JOIN pos_core.purchase_order po ON po.id = g.purchase_order_id AND po.is_active = true " +
            "LEFT JOIN pos_core.supplier s ON s.id = po.supplier_id AND s.is_active = true " +
            "WHERE g.shop_id = :shopId AND g.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(g.grn_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(po.po_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(s.supplier_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(g.reference_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:grnStatus AS text) IS NULL OR g.grn_status = CAST(:grnStatus AS text)) AND " +
            "(CAST(:supplierId AS uuid) IS NULL OR po.supplier_id = CAST(:supplierId AS uuid)) AND " +
            "(CAST(:purchaseOrderId AS uuid) IS NULL OR g.purchase_order_id = CAST(:purchaseOrderId AS uuid)) AND " +
            "(CAST(:dateFrom AS timestamp) IS NULL OR g.created_at >= CAST(:dateFrom AS timestamp)) AND " +
            "(CAST(:dateTo AS timestamp) IS NULL OR g.created_at <= CAST(:dateTo AS timestamp)) " +
            "ORDER BY g.created_at DESC",
            nativeQuery = true)
    List<GoodsReceivedNote> searchFilteredUnpaged(@Param("shopId") UUID shopId,
                                                   @Param("search") String search,
                                                   @Param("grnStatus") String grnStatus,
                                                   @Param("supplierId") UUID supplierId,
                                                   @Param("purchaseOrderId") UUID purchaseOrderId,
                                                   @Param("dateFrom") LocalDateTime dateFrom,
                                                   @Param("dateTo") LocalDateTime dateTo);
}
