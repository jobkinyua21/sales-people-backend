package com.possystem.supplier;

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
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    long countByShopId(UUID shopId);

    boolean existsByShopIdAndSupplierCode(UUID shopId, String supplierCode);

    Optional<Supplier> findByIdAndShopIdAndIsActiveTrue(UUID id, UUID shopId);

    boolean existsByShopIdAndSupplierNameIgnoreCaseAndIsActiveTrue(UUID shopId, String supplierName);

    boolean existsByShopIdAndSupplierNameIgnoreCaseAndIsActiveTrueAndIdNot(UUID shopId, String supplierName, UUID id);

    boolean existsByShopIdAndEmailIgnoreCaseAndIsActiveTrue(UUID shopId, String email);

    boolean existsByShopIdAndEmailIgnoreCaseAndIsActiveTrueAndIdNot(UUID shopId, String email, UUID id);

    List<Supplier> findAllByIdInAndShopIdAndIsActiveTrue(List<UUID> ids, UUID shopId);

    @Query("SELECT s FROM Supplier s WHERE s.shopId = :shopId AND s.isActive = true AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(s.supplierCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.contactNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.tinNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY s.createdAt DESC")
    Page<Supplier> searchAll(@Param("shopId") UUID shopId,
                             @Param("search") String search,
                             Pageable pageable);

    @Query("SELECT s FROM Supplier s WHERE s.shopId = :shopId AND s.isActive = true AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(s.supplierCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.contactNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.tinNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY s.createdAt DESC")
    List<Supplier> searchAll(@Param("shopId") UUID shopId,
                             @Param("search") String search);
}
