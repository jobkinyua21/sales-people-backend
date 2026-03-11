package com.possystem.kitchen.recipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    Optional<Recipe> findByIdAndShopIdAndIsActiveTrue(UUID id, UUID shopId);

    List<Recipe> findAllByIdInAndShopIdAndIsActiveTrue(List<UUID> ids, UUID shopId);

    Optional<Recipe> findByVariantIdAndShopIdAndIsActiveTrue(UUID variantId, UUID shopId);

    List<Recipe> findAllByShopIdAndIsActiveTrueOrderByRecipeNameAsc(UUID shopId);

    List<Recipe> findAllByShopIdAndStatusAndIsActiveTrueOrderByRecipeNameAsc(UUID shopId, RecipeStatus status);

    long countByShopId(UUID shopId);

    @Query(value = "SELECT r.* FROM pos_core.recipe r " +
            "WHERE r.shop_id = :shopId AND r.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(r.recipe_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:status AS text) IS NULL OR CAST(:status AS text) = '' OR r.status = CAST(:status AS text)) AND " +
            "(CAST(:prepStation AS text) IS NULL OR CAST(:prepStation AS text) = '' OR r.prep_station = CAST(:prepStation AS text)) AND " +
            "(CAST(:variantId AS uuid) IS NULL OR r.variant_id = CAST(:variantId AS uuid)) " +
            "ORDER BY r.recipe_name ASC " +
            "LIMIT :limit OFFSET :start",
            nativeQuery = true)
    List<Recipe> searchFiltered(@Param("shopId") UUID shopId,
                                @Param("search") String search,
                                @Param("status") String status,
                                @Param("prepStation") String prepStation,
                                @Param("variantId") UUID variantId,
                                @Param("start") int start,
                                @Param("limit") int limit);
}
