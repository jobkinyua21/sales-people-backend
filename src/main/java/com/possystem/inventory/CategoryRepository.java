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
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    long countByShopId(UUID shopId);

    boolean existsByShopIdAndCategoryCode(UUID shopId, String categoryCode);

    Optional<Category> findByIdAndShopIdAndIsActiveTrue(UUID id, UUID shopId);

    boolean existsByShopIdAndCategoryNameIgnoreCaseAndIsActiveTrue(UUID shopId, String categoryName);

    boolean existsByShopIdAndCategoryNameIgnoreCaseAndIsActiveTrueAndIdNot(UUID shopId, String categoryName, UUID id);

    @Query("SELECT c FROM Category c WHERE c.shopId = :shopId AND c.isActive = true AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(c.categoryCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(c.status AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY c.sortOrder ASC, c.createdAt DESC")
    Page<Category> searchAll(@Param("shopId") UUID shopId,
                             @Param("search") String search,
                             Pageable pageable);

    @Query("SELECT c FROM Category c WHERE c.shopId = :shopId AND c.isActive = true AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(c.categoryCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(c.status AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY c.sortOrder ASC, c.createdAt DESC")
    List<Category> searchAll(@Param("shopId") UUID shopId,
                             @Param("search") String search);
}
