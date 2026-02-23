package com.possystem.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, UUID> {

    Optional<InventoryStock> findByVariantIdAndShopIdAndIsActiveTrue(UUID variantId, UUID shopId);

    Optional<InventoryStock> findByIdAndShopIdAndIsActiveTrue(UUID id, UUID shopId);

    List<InventoryStock> findByShopIdAndIsActiveTrue(UUID shopId);

    List<InventoryStock> findAllByIdInAndShopIdAndIsActiveTrue(List<UUID> ids, UUID shopId);

    @Query(value = "SELECT s.* FROM pos_core.inventory_stock s " +
            "JOIN pos_core.product_variant v ON v.id = s.variant_id AND v.is_active = true " +
            "JOIN pos_core.product p ON p.id = v.product_id AND p.is_active = true " +
            "WHERE s.shop_id = :shopId AND s.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(p.product_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(v.sku) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(v.variant_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(v.barcode) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:categoryId AS uuid) IS NULL OR p.category_id = CAST(:categoryId AS uuid)) AND " +
            "(CAST(:stockStatus AS text) IS NULL OR CAST(:stockStatus AS text) = '' OR " +
            "(CAST(:stockStatus AS text) = 'OUT_OF_STOCK' AND s.current_quantity <= 0) OR " +
            "(CAST(:stockStatus AS text) = 'LOW_STOCK' AND s.reorder_level IS NOT NULL AND s.current_quantity > 0 AND s.current_quantity <= s.reorder_level) OR " +
            "(CAST(:stockStatus AS text) = 'IN_STOCK' AND (s.reorder_level IS NULL OR s.current_quantity > s.reorder_level))) " +
            "ORDER BY p.product_name ASC, v.sort_order ASC",
            countQuery = "SELECT COUNT(s.id) FROM pos_core.inventory_stock s " +
            "JOIN pos_core.product_variant v ON v.id = s.variant_id AND v.is_active = true " +
            "JOIN pos_core.product p ON p.id = v.product_id AND p.is_active = true " +
            "WHERE s.shop_id = :shopId AND s.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(p.product_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(v.sku) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(v.variant_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(v.barcode) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:categoryId AS uuid) IS NULL OR p.category_id = CAST(:categoryId AS uuid)) AND " +
            "(CAST(:stockStatus AS text) IS NULL OR CAST(:stockStatus AS text) = '' OR " +
            "(CAST(:stockStatus AS text) = 'OUT_OF_STOCK' AND s.current_quantity <= 0) OR " +
            "(CAST(:stockStatus AS text) = 'LOW_STOCK' AND s.reorder_level IS NOT NULL AND s.current_quantity > 0 AND s.current_quantity <= s.reorder_level) OR " +
            "(CAST(:stockStatus AS text) = 'IN_STOCK' AND (s.reorder_level IS NULL OR s.current_quantity > s.reorder_level)))",
            nativeQuery = true)
    Page<InventoryStock> searchFiltered(@Param("shopId") UUID shopId,
                                        @Param("search") String search,
                                        @Param("categoryId") UUID categoryId,
                                        @Param("stockStatus") String stockStatus,
                                        Pageable pageable);

    @Query(value = "SELECT s.* FROM pos_core.inventory_stock s " +
            "JOIN pos_core.product_variant v ON v.id = s.variant_id AND v.is_active = true " +
            "JOIN pos_core.product p ON p.id = v.product_id AND p.is_active = true " +
            "WHERE s.shop_id = :shopId AND s.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(p.product_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(v.sku) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(v.variant_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(v.barcode) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:categoryId AS uuid) IS NULL OR p.category_id = CAST(:categoryId AS uuid)) AND " +
            "(CAST(:stockStatus AS text) IS NULL OR CAST(:stockStatus AS text) = '' OR " +
            "(CAST(:stockStatus AS text) = 'OUT_OF_STOCK' AND s.current_quantity <= 0) OR " +
            "(CAST(:stockStatus AS text) = 'LOW_STOCK' AND s.reorder_level IS NOT NULL AND s.current_quantity > 0 AND s.current_quantity <= s.reorder_level) OR " +
            "(CAST(:stockStatus AS text) = 'IN_STOCK' AND (s.reorder_level IS NULL OR s.current_quantity > s.reorder_level))) " +
            "ORDER BY p.product_name ASC, v.sort_order ASC",
            nativeQuery = true)
    List<InventoryStock> searchFiltered(@Param("shopId") UUID shopId,
                                        @Param("search") String search,
                                        @Param("categoryId") UUID categoryId,
                                        @Param("stockStatus") String stockStatus);
}
