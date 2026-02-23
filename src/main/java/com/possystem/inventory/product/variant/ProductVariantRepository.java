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
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    long countByShopId(UUID shopId);

    boolean existsByShopIdAndSku(UUID shopId, String sku);

    Optional<ProductVariant> findByIdAndShopIdAndIsActiveTrue(UUID id, UUID shopId);

    List<ProductVariant> findByProductIdAndShopIdAndIsActiveTrueOrderBySortOrderAsc(UUID productId, UUID shopId);

    Optional<ProductVariant> findByProductIdAndShopIdAndIsDefaultTrueAndIsActiveTrue(UUID productId, UUID shopId);

    boolean existsByShopIdAndSkuAndIsActiveTrueAndIdNot(UUID shopId, String sku, UUID id);

    @Query("SELECT v FROM ProductVariant v WHERE v.shopId = :shopId AND v.isActive = true AND " +
            "(:productId IS NULL OR v.productId = :productId) AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(v.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(v.variantName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(v.barcode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY v.sortOrder ASC, v.createdAt DESC")
    Page<ProductVariant> searchAll(@Param("shopId") UUID shopId,
                                   @Param("productId") UUID productId,
                                   @Param("search") String search,
                                   Pageable pageable);

    @Query("SELECT v FROM ProductVariant v WHERE v.shopId = :shopId AND v.isActive = true AND " +
            "(:productId IS NULL OR v.productId = :productId) AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(v.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(v.variantName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(v.barcode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY v.sortOrder ASC, v.createdAt DESC")
    List<ProductVariant> searchAll(@Param("shopId") UUID shopId,
                                   @Param("productId") UUID productId,
                                   @Param("search") String search);
}
