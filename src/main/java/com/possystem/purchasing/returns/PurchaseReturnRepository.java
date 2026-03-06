package com.possystem.purchasing.returns;

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
public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, UUID> {

    long countByShopId(UUID shopId);

    boolean existsByShopIdAndReturnNumber(UUID shopId, String returnNumber);

    Optional<PurchaseReturn> findByIdAndShopIdAndIsActiveTrue(UUID id, UUID shopId);

    List<PurchaseReturn> findAllByIdInAndShopIdAndIsActiveTrue(List<UUID> ids, UUID shopId);

    @Query(value = "SELECT pr.* FROM pos_core.purchase_return pr " +
            "LEFT JOIN pos_core.supplier s ON s.id = pr.supplier_id AND s.is_active = true " +
            "LEFT JOIN pos_core.purchase_order po ON po.id = pr.purchase_order_id AND po.is_active = true " +
            "LEFT JOIN pos_core.goods_received_note grn ON grn.id = pr.grn_id AND grn.is_active = true " +
            "WHERE pr.shop_id = :shopId AND pr.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(pr.return_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(s.supplier_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(po.po_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(grn.grn_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(pr.reference_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:returnStatus AS text) IS NULL OR pr.return_status = CAST(:returnStatus AS text)) AND " +
            "(CAST(:supplierId AS uuid) IS NULL OR pr.supplier_id = CAST(:supplierId AS uuid)) AND " +
            "(CAST(:grnId AS uuid) IS NULL OR pr.grn_id = CAST(:grnId AS uuid)) AND " +
            "(CAST(:purchaseOrderId AS uuid) IS NULL OR pr.purchase_order_id = CAST(:purchaseOrderId AS uuid)) AND " +
            "(CAST(:dateFrom AS timestamp) IS NULL OR pr.created_at >= CAST(:dateFrom AS timestamp)) AND " +
            "(CAST(:dateTo AS timestamp) IS NULL OR pr.created_at <= CAST(:dateTo AS timestamp)) " +
            "ORDER BY pr.created_at DESC",
            countQuery = "SELECT COUNT(pr.id) FROM pos_core.purchase_return pr " +
                    "LEFT JOIN pos_core.supplier s ON s.id = pr.supplier_id AND s.is_active = true " +
                    "LEFT JOIN pos_core.purchase_order po ON po.id = pr.purchase_order_id AND po.is_active = true " +
                    "LEFT JOIN pos_core.goods_received_note grn ON grn.id = pr.grn_id AND grn.is_active = true " +
                    "WHERE pr.shop_id = :shopId AND pr.is_active = true AND " +
                    "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
                    "LOWER(pr.return_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
                    "LOWER(s.supplier_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
                    "LOWER(po.po_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
                    "LOWER(grn.grn_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
                    "LOWER(pr.reference_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
                    "(CAST(:returnStatus AS text) IS NULL OR pr.return_status = CAST(:returnStatus AS text)) AND " +
                    "(CAST(:supplierId AS uuid) IS NULL OR pr.supplier_id = CAST(:supplierId AS uuid)) AND " +
                    "(CAST(:grnId AS uuid) IS NULL OR pr.grn_id = CAST(:grnId AS uuid)) AND " +
                    "(CAST(:purchaseOrderId AS uuid) IS NULL OR pr.purchase_order_id = CAST(:purchaseOrderId AS uuid)) AND " +
                    "(CAST(:dateFrom AS timestamp) IS NULL OR pr.created_at >= CAST(:dateFrom AS timestamp)) AND " +
                    "(CAST(:dateTo AS timestamp) IS NULL OR pr.created_at <= CAST(:dateTo AS timestamp))",
            nativeQuery = true)
    Page<PurchaseReturn> searchFiltered(@Param("shopId") UUID shopId,
                                         @Param("search") String search,
                                         @Param("returnStatus") String returnStatus,
                                         @Param("supplierId") UUID supplierId,
                                         @Param("grnId") UUID grnId,
                                         @Param("purchaseOrderId") UUID purchaseOrderId,
                                         @Param("dateFrom") LocalDateTime dateFrom,
                                         @Param("dateTo") LocalDateTime dateTo,
                                         Pageable pageable);

    @Query(value = "SELECT pr.* FROM pos_core.purchase_return pr " +
            "LEFT JOIN pos_core.supplier s ON s.id = pr.supplier_id AND s.is_active = true " +
            "LEFT JOIN pos_core.purchase_order po ON po.id = pr.purchase_order_id AND po.is_active = true " +
            "LEFT JOIN pos_core.goods_received_note grn ON grn.id = pr.grn_id AND grn.is_active = true " +
            "WHERE pr.shop_id = :shopId AND pr.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(pr.return_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(s.supplier_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(po.po_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(grn.grn_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(pr.reference_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:returnStatus AS text) IS NULL OR pr.return_status = CAST(:returnStatus AS text)) AND " +
            "(CAST(:supplierId AS uuid) IS NULL OR pr.supplier_id = CAST(:supplierId AS uuid)) AND " +
            "(CAST(:grnId AS uuid) IS NULL OR pr.grn_id = CAST(:grnId AS uuid)) AND " +
            "(CAST(:purchaseOrderId AS uuid) IS NULL OR pr.purchase_order_id = CAST(:purchaseOrderId AS uuid)) AND " +
            "(CAST(:dateFrom AS timestamp) IS NULL OR pr.created_at >= CAST(:dateFrom AS timestamp)) AND " +
            "(CAST(:dateTo AS timestamp) IS NULL OR pr.created_at <= CAST(:dateTo AS timestamp)) " +
            "ORDER BY pr.created_at DESC",
            nativeQuery = true)
    List<PurchaseReturn> searchFilteredUnpaged(@Param("shopId") UUID shopId,
                                                @Param("search") String search,
                                                @Param("returnStatus") String returnStatus,
                                                @Param("supplierId") UUID supplierId,
                                                @Param("grnId") UUID grnId,
                                                @Param("purchaseOrderId") UUID purchaseOrderId,
                                                @Param("dateFrom") LocalDateTime dateFrom,
                                                @Param("dateTo") LocalDateTime dateTo);
}
