package com.possystem.shop;

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
public interface ShopRepository extends JpaRepository<Shop, UUID> {

    boolean existsByShopCode(String shopCode);

    Optional<Shop> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT s FROM Shop s WHERE s.tenantId = :tenantId AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(s.shopCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.shopName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.address) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.city) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.country) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(s.status AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY s.createdAt DESC")
    Page<Shop> searchAll(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT s FROM Shop s WHERE s.tenantId = :tenantId AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(s.shopCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.shopName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.address) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.city) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.country) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(s.status AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY s.createdAt DESC")
    List<Shop> searchAll(@Param("tenantId") UUID tenantId, @Param("search") String search);

    long countByTenantId(UUID tenantId);
}
