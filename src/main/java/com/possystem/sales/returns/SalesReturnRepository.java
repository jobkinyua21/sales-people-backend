package com.possystem.sales.returns;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesReturnRepository extends JpaRepository<SalesReturn, UUID> {

    Optional<SalesReturn> findByIdAndShopId(UUID id, UUID shopId);

    List<SalesReturn> findAllByShopIdOrderByCreatedAtDesc(UUID shopId);

    List<SalesReturn> findAllByShopIdAndStatusOrderByCreatedAtDesc(UUID shopId, ReturnStatus status);

    List<SalesReturn> findAllByOrderIdAndShopId(UUID orderId, UUID shopId);

    long countByShopId(UUID shopId);

    @Query("SELECT COALESCE(SUM(ri.quantityReturned), 0) FROM SalesReturnItem ri " +
            "JOIN ri.salesReturn r WHERE r.orderId = :orderId AND ri.orderItemId = :orderItemId " +
            "AND r.status IN ('APPROVED', 'COMPLETED')")
    java.math.BigDecimal totalReturnedQuantity(UUID orderId, UUID orderItemId);
}
