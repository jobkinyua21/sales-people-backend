package com.possystem.customer;

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
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    long countByShopId(UUID shopId);

    boolean existsByShopIdAndCustomerCode(UUID shopId, String customerCode);

    Optional<Customer> findByIdAndShopIdAndIsActiveTrue(UUID id, UUID shopId);

    boolean existsByShopIdAndCustomerNameIgnoreCaseAndIsActiveTrue(UUID shopId, String customerName);

    boolean existsByShopIdAndCustomerNameIgnoreCaseAndIsActiveTrueAndIdNot(UUID shopId, String customerName, UUID id);

    boolean existsByShopIdAndEmailIgnoreCaseAndIsActiveTrue(UUID shopId, String email);

    boolean existsByShopIdAndEmailIgnoreCaseAndIsActiveTrueAndIdNot(UUID shopId, String email, UUID id);

    List<Customer> findAllByIdInAndShopIdAndIsActiveTrue(List<UUID> ids, UUID shopId);

    @Query("SELECT c FROM Customer c WHERE c.shopId = :shopId AND c.isActive = true AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.contactNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.tinNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY c.createdAt DESC")
    Page<Customer> searchAll(@Param("shopId") UUID shopId,
                             @Param("search") String search,
                             Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.shopId = :shopId AND c.isActive = true AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.contactNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.tinNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY c.createdAt DESC")
    List<Customer> searchAll(@Param("shopId") UUID shopId,
                             @Param("search") String search);
}
