package com.salespeople.salesorder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalesOrderHeaderRepository extends JpaRepository<SalesOrderHeader, Long> {

    Optional<SalesOrderHeader> findBySalesOrderNumber(Long salesOrderNumber);

    List<SalesOrderHeader> findBySalesPersonNumberAndSalesOrderDateAndStatus(
            Integer salesPersonNumber, LocalDate salesOrderDate, SalesOrderStatus status);

    @Query("""
            SELECT h FROM SalesOrderHeader h
            WHERE (:salesPersonNumber IS NULL OR h.salesPersonNumber = :salesPersonNumber)
            AND (COALESCE(:status, '') = '' OR LOWER(h.status) = LOWER(:status))
            AND (COALESCE(:search, '') = ''
                 OR LOWER(h.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR CAST(h.salesOrderNumber AS string) LIKE CONCAT('%', :search, '%'))
            ORDER BY h.entryDate DESC
            """)
    List<SalesOrderHeader> searchAll(
            @Param("salesPersonNumber") Integer salesPersonNumber,
            @Param("status") String status,
            @Param("search") String search);

    @Query("""
            SELECT h FROM SalesOrderHeader h
            WHERE (:salesPersonNumber IS NULL OR h.salesPersonNumber = :salesPersonNumber)
            AND (COALESCE(:status, '') = '' OR LOWER(h.status) = LOWER(:status))
            AND (COALESCE(:search, '') = ''
                 OR LOWER(h.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR CAST(h.salesOrderNumber AS string) LIKE CONCAT('%', :search, '%'))
            ORDER BY h.entryDate DESC
            """)
    Page<SalesOrderHeader> searchAll(
            @Param("salesPersonNumber") Integer salesPersonNumber,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
}
