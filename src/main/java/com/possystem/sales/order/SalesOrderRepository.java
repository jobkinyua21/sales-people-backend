package com.possystem.sales;

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
public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID> {

    long countByShopId(UUID shopId);

    boolean existsByShopIdAndOrderNumber(UUID shopId, String orderNumber);

    Optional<SalesOrder> findByIdAndShopIdAndIsActiveTrue(UUID id, UUID shopId);

    List<SalesOrder> findAllByIdInAndShopIdAndIsActiveTrue(List<UUID> ids, UUID shopId);

    @Query(value = "SELECT o.* FROM pos_core.sales_order o " +
            "LEFT JOIN pos_core.customer c ON c.id = o.customer_id AND c.is_active = true " +
            "WHERE o.shop_id = :shopId AND o.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(o.order_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(c.customer_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(o.reference_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:orderStatus AS text) IS NULL OR o.order_status = CAST(:orderStatus AS text)) AND " +
            "(CAST(:paymentStatus AS text) IS NULL OR o.payment_status = CAST(:paymentStatus AS text)) AND " +
            "(CAST(:paymentMethod AS text) IS NULL OR EXISTS (" +
            "SELECT 1 FROM pos_core.sales_payment sp WHERE sp.sales_order_id = o.id AND sp.payment_method = CAST(:paymentMethod AS text))) AND " +
            "(CAST(:customerId AS uuid) IS NULL OR o.customer_id = CAST(:customerId AS uuid)) AND " +
            "(CAST(:dateFrom AS timestamp) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamp)) AND " +
            "(CAST(:dateTo AS timestamp) IS NULL OR o.created_at <= CAST(:dateTo AS timestamp)) " +
            "ORDER BY o.created_at DESC",
            countQuery = "SELECT COUNT(o.id) FROM pos_core.sales_order o " +
            "LEFT JOIN pos_core.customer c ON c.id = o.customer_id AND c.is_active = true " +
            "WHERE o.shop_id = :shopId AND o.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(o.order_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(c.customer_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(o.reference_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:orderStatus AS text) IS NULL OR o.order_status = CAST(:orderStatus AS text)) AND " +
            "(CAST(:paymentStatus AS text) IS NULL OR o.payment_status = CAST(:paymentStatus AS text)) AND " +
            "(CAST(:paymentMethod AS text) IS NULL OR EXISTS (" +
            "SELECT 1 FROM pos_core.sales_payment sp WHERE sp.sales_order_id = o.id AND sp.payment_method = CAST(:paymentMethod AS text))) AND " +
            "(CAST(:customerId AS uuid) IS NULL OR o.customer_id = CAST(:customerId AS uuid)) AND " +
            "(CAST(:dateFrom AS timestamp) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamp)) AND " +
            "(CAST(:dateTo AS timestamp) IS NULL OR o.created_at <= CAST(:dateTo AS timestamp))",
            nativeQuery = true)
    Page<SalesOrder> searchFiltered(@Param("shopId") UUID shopId,
                                    @Param("search") String search,
                                    @Param("orderStatus") String orderStatus,
                                    @Param("paymentStatus") String paymentStatus,
                                    @Param("paymentMethod") String paymentMethod,
                                    @Param("customerId") UUID customerId,
                                    @Param("dateFrom") LocalDateTime dateFrom,
                                    @Param("dateTo") LocalDateTime dateTo,
                                    Pageable pageable);

    @Query(value = "SELECT o.* FROM pos_core.sales_order o " +
            "LEFT JOIN pos_core.customer c ON c.id = o.customer_id AND c.is_active = true " +
            "WHERE o.shop_id = :shopId AND o.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(o.order_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(c.customer_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(o.reference_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:orderStatus AS text) IS NULL OR o.order_status = CAST(:orderStatus AS text)) AND " +
            "(CAST(:paymentStatus AS text) IS NULL OR o.payment_status = CAST(:paymentStatus AS text)) AND " +
            "(CAST(:paymentMethod AS text) IS NULL OR EXISTS (" +
            "SELECT 1 FROM pos_core.sales_payment sp WHERE sp.sales_order_id = o.id AND sp.payment_method = CAST(:paymentMethod AS text))) AND " +
            "(CAST(:customerId AS uuid) IS NULL OR o.customer_id = CAST(:customerId AS uuid)) AND " +
            "(CAST(:dateFrom AS timestamp) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamp)) AND " +
            "(CAST(:dateTo AS timestamp) IS NULL OR o.created_at <= CAST(:dateTo AS timestamp)) " +
            "ORDER BY o.created_at DESC",
            nativeQuery = true)
    List<SalesOrder> searchFiltered(@Param("shopId") UUID shopId,
                                    @Param("search") String search,
                                    @Param("orderStatus") String orderStatus,
                                    @Param("paymentStatus") String paymentStatus,
                                    @Param("paymentMethod") String paymentMethod,
                                    @Param("customerId") UUID customerId,
                                    @Param("dateFrom") LocalDateTime dateFrom,
                                    @Param("dateTo") LocalDateTime dateTo);
}
