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
public interface ProductRepository extends JpaRepository<Product, UUID> {

    long countByShopId(UUID shopId);

    boolean existsByShopIdAndProductCode(UUID shopId, String productCode);

    Optional<Product> findByIdAndShopIdAndIsActiveTrue(UUID id, UUID shopId);

    boolean existsByShopIdAndProductNameIgnoreCaseAndIsActiveTrue(UUID shopId, String productName);

    boolean existsByShopIdAndProductNameIgnoreCaseAndIsActiveTrueAndIdNot(UUID shopId, String productName, UUID id);

    List<Product> findAllByIdInAndShopIdAndIsActiveTrue(List<UUID> ids, UUID shopId);

    @Query("SELECT p FROM Product p WHERE p.shopId = :shopId AND p.isActive = true AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(p.productCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(p.productType AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(p.status AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY p.createdAt DESC")
    Page<Product> searchAll(@Param("shopId") UUID shopId,
                            @Param("search") String search,
                            Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.shopId = :shopId AND p.isActive = true AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(p.productCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(p.productType AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(p.status AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY p.createdAt DESC")
    List<Product> searchAll(@Param("shopId") UUID shopId,
                            @Param("search") String search);
}
