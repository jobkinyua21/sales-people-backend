package com.salespeople.batchorder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalesPersonBatchOrderRepository extends JpaRepository<SalesPersonBatchOrder, Long> {

    List<SalesPersonBatchOrder> findByBatchRef(String batchRef);

    List<SalesPersonBatchOrder> findBySalesPersonNumber(Integer salesPersonNumber);

    List<SalesPersonBatchOrder> findByStatus(SalesPersonBatchOrderStatus status);

    Optional<SalesPersonBatchOrder> findBySalesOrderHeaderId(Long salesOrderHeaderId);

    @Query("""
            SELECT b FROM SalesPersonBatchOrder b
            WHERE (:salesPersonNumber IS NULL OR b.salesPersonNumber = :salesPersonNumber)
            AND (:status IS NULL OR b.status = :status)
            AND (:batchRef IS NULL OR b.batchRef = :batchRef)
            ORDER BY b.createdAt DESC
            """)
    List<SalesPersonBatchOrder> search(
            @Param("salesPersonNumber") Integer salesPersonNumber,
            @Param("status") SalesPersonBatchOrderStatus status,
            @Param("batchRef") String batchRef);
}
