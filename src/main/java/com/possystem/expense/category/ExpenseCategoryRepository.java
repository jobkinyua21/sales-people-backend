package com.possystem.expense.category;

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
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, UUID> {

    long countByShopId(UUID shopId);

    boolean existsByShopIdAndCategoryCode(UUID shopId, String categoryCode);

    Optional<ExpenseCategory> findByIdAndShopIdAndIsActiveTrue(UUID id, UUID shopId);

    boolean existsByShopIdAndCategoryNameIgnoreCaseAndIsActiveTrue(UUID shopId, String categoryName);

    boolean existsByShopIdAndCategoryNameIgnoreCaseAndIsActiveTrueAndIdNot(UUID shopId, String categoryName, UUID id);

    List<ExpenseCategory> findAllByIdInAndShopIdAndIsActiveTrue(List<UUID> ids, UUID shopId);

    @Query("SELECT c FROM ExpenseCategory c WHERE c.shopId = :shopId AND c.isActive = true AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(c.categoryCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY c.createdAt DESC")
    Page<ExpenseCategory> searchAll(@Param("shopId") UUID shopId,
                                     @Param("search") String search,
                                     Pageable pageable);

    @Query("SELECT c FROM ExpenseCategory c WHERE c.shopId = :shopId AND c.isActive = true AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(c.categoryCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY c.createdAt DESC")
    List<ExpenseCategory> searchAll(@Param("shopId") UUID shopId,
                                     @Param("search") String search);
}
