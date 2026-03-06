package com.possystem.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    long countByShopId(UUID shopId);

    boolean existsByShopIdAndExpenseNumber(UUID shopId, String expenseNumber);

    Optional<Expense> findByIdAndShopIdAndIsActiveTrue(UUID id, UUID shopId);

    List<Expense> findAllByIdInAndShopIdAndIsActiveTrue(List<UUID> ids, UUID shopId);

    @Query(value = "SELECT e.* FROM pos_core.expense e " +
            "LEFT JOIN pos_core.expense_category ec ON ec.id = e.expense_category_id AND ec.is_active = true " +
            "WHERE e.shop_id = :shopId AND e.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(e.expense_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(e.vendor) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(e.reference_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(ec.category_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:expenseStatus AS text) IS NULL OR e.expense_status = CAST(:expenseStatus AS text)) AND " +
            "(CAST(:expenseCategoryId AS uuid) IS NULL OR e.expense_category_id = CAST(:expenseCategoryId AS uuid)) AND " +
            "(CAST(:paymentMethod AS text) IS NULL OR e.payment_method = CAST(:paymentMethod AS text)) AND " +
            "(CAST(:dateFrom AS timestamp) IS NULL OR e.expense_date >= CAST(:dateFrom AS date)) AND " +
            "(CAST(:dateTo AS timestamp) IS NULL OR e.expense_date <= CAST(:dateTo AS date)) " +
            "ORDER BY e.created_at DESC",
            countQuery = "SELECT COUNT(e.id) FROM pos_core.expense e " +
                    "LEFT JOIN pos_core.expense_category ec ON ec.id = e.expense_category_id AND ec.is_active = true " +
                    "WHERE e.shop_id = :shopId AND e.is_active = true AND " +
                    "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
                    "LOWER(e.expense_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
                    "LOWER(e.vendor) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
                    "LOWER(e.reference_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
                    "LOWER(e.description) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
                    "LOWER(ec.category_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
                    "(CAST(:expenseStatus AS text) IS NULL OR e.expense_status = CAST(:expenseStatus AS text)) AND " +
                    "(CAST(:expenseCategoryId AS uuid) IS NULL OR e.expense_category_id = CAST(:expenseCategoryId AS uuid)) AND " +
                    "(CAST(:paymentMethod AS text) IS NULL OR e.payment_method = CAST(:paymentMethod AS text)) AND " +
                    "(CAST(:dateFrom AS timestamp) IS NULL OR e.expense_date >= CAST(:dateFrom AS date)) AND " +
                    "(CAST(:dateTo AS timestamp) IS NULL OR e.expense_date <= CAST(:dateTo AS date))",
            nativeQuery = true)
    Page<Expense> searchFiltered(@Param("shopId") UUID shopId,
                                  @Param("search") String search,
                                  @Param("expenseStatus") String expenseStatus,
                                  @Param("expenseCategoryId") UUID expenseCategoryId,
                                  @Param("paymentMethod") String paymentMethod,
                                  @Param("dateFrom") LocalDateTime dateFrom,
                                  @Param("dateTo") LocalDateTime dateTo,
                                  Pageable pageable);

    @Query(value = "SELECT e.* FROM pos_core.expense e " +
            "LEFT JOIN pos_core.expense_category ec ON ec.id = e.expense_category_id AND ec.is_active = true " +
            "WHERE e.shop_id = :shopId AND e.is_active = true AND " +
            "(CAST(:search AS text) IS NULL OR CAST(:search AS text) = '' OR " +
            "LOWER(e.expense_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(e.vendor) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(e.reference_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
            "LOWER(ec.category_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(CAST(:expenseStatus AS text) IS NULL OR e.expense_status = CAST(:expenseStatus AS text)) AND " +
            "(CAST(:expenseCategoryId AS uuid) IS NULL OR e.expense_category_id = CAST(:expenseCategoryId AS uuid)) AND " +
            "(CAST(:paymentMethod AS text) IS NULL OR e.payment_method = CAST(:paymentMethod AS text)) AND " +
            "(CAST(:dateFrom AS timestamp) IS NULL OR e.expense_date >= CAST(:dateFrom AS date)) AND " +
            "(CAST(:dateTo AS timestamp) IS NULL OR e.expense_date <= CAST(:dateTo AS date)) " +
            "ORDER BY e.created_at DESC",
            nativeQuery = true)
    List<Expense> searchFilteredUnpaged(@Param("shopId") UUID shopId,
                                         @Param("search") String search,
                                         @Param("expenseStatus") String expenseStatus,
                                         @Param("expenseCategoryId") UUID expenseCategoryId,
                                         @Param("paymentMethod") String paymentMethod,
                                         @Param("dateFrom") LocalDateTime dateFrom,
                                         @Param("dateTo") LocalDateTime dateTo);

    // ==================== SUMMARY / ANALYTICS ====================

    @Query(value = "SELECT EXTRACT(YEAR FROM e.expense_date) AS year, " +
            "EXTRACT(MONTH FROM e.expense_date) AS month, " +
            "SUM(e.amount) AS total " +
            "FROM pos_core.expense e " +
            "WHERE e.shop_id = :shopId AND e.is_active = true " +
            "AND e.expense_status IN ('APPROVED', 'PAID') " +
            "AND e.expense_date >= :dateFrom AND e.expense_date <= :dateTo " +
            "GROUP BY EXTRACT(YEAR FROM e.expense_date), EXTRACT(MONTH FROM e.expense_date) " +
            "ORDER BY year, month",
            nativeQuery = true)
    List<ExpenseMonthlySummary> getMonthlyTotals(@Param("shopId") UUID shopId,
                                                  @Param("dateFrom") LocalDate dateFrom,
                                                  @Param("dateTo") LocalDate dateTo);

    @Query(value = "SELECT EXTRACT(YEAR FROM e.expense_date) AS year, " +
            "EXTRACT(MONTH FROM e.expense_date) AS month, " +
            "e.expense_category_id AS categoryId, " +
            "ec.category_name AS categoryName, " +
            "SUM(e.amount) AS total " +
            "FROM pos_core.expense e " +
            "LEFT JOIN pos_core.expense_category ec ON ec.id = e.expense_category_id " +
            "WHERE e.shop_id = :shopId AND e.is_active = true " +
            "AND e.expense_status IN ('APPROVED', 'PAID') " +
            "AND e.expense_date >= :dateFrom AND e.expense_date <= :dateTo " +
            "GROUP BY EXTRACT(YEAR FROM e.expense_date), EXTRACT(MONTH FROM e.expense_date), " +
            "e.expense_category_id, ec.category_name " +
            "ORDER BY year, month, ec.category_name",
            nativeQuery = true)
    List<ExpenseCategorySummary> getMonthlyCategoryTotals(@Param("shopId") UUID shopId,
                                                          @Param("dateFrom") LocalDate dateFrom,
                                                          @Param("dateTo") LocalDate dateTo);
}
