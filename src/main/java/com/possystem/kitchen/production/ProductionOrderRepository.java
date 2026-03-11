package com.possystem.kitchen.production;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, UUID> {

    Optional<ProductionOrder> findByIdAndShopIdAndIsActiveTrue(UUID id, UUID shopId);

    List<ProductionOrder> findAllByShopIdAndStatusAndIsActiveTrueOrderByCreatedAtDesc(UUID shopId, ProductionStatus status);

    List<ProductionOrder> findAllByShopIdAndIsActiveTrueOrderByCreatedAtDesc(UUID shopId);

    long countByShopId(UUID shopId);

    @Query(value = "SELECT po.* FROM pos_core.production_order po " +
            "WHERE po.shop_id = :shopId AND po.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(po.recipe_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(po.product_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(po.production_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:status AS text) IS NULL OR CAST(:status AS text) = '' OR po.status = CAST(:status AS text)) AND " +
            "(CAST(:recipeId AS uuid) IS NULL OR po.recipe_id = CAST(:recipeId AS uuid)) " +
            "ORDER BY po.created_at DESC " +
            "LIMIT :limit OFFSET :start",
            nativeQuery = true)
    List<ProductionOrder> searchFiltered(@Param("shopId") UUID shopId,
                                         @Param("search") String search,
                                         @Param("status") String status,
                                         @Param("recipeId") UUID recipeId,
                                         @Param("start") int start,
                                         @Param("limit") int limit);
}
