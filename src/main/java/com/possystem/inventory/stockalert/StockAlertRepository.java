package com.possystem.inventory.stockalert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, UUID> {

    Optional<StockAlert> findByIdAndShopId(UUID id, UUID shopId);

    List<StockAlert> findAllByShopIdAndStatusOrderByCreatedAtDesc(UUID shopId, StockAlertStatus status);

    List<StockAlert> findAllByShopIdOrderByCreatedAtDesc(UUID shopId);

    boolean existsByVariantIdAndShopIdAndAlertTypeAndStatus(UUID variantId, UUID shopId, StockAlertType alertType, StockAlertStatus status);

    Optional<StockAlert> findByVariantIdAndShopIdAndAlertTypeAndStatus(UUID variantId, UUID shopId, StockAlertType alertType, StockAlertStatus status);

    @Query("SELECT COUNT(a) FROM StockAlert a WHERE a.shopId = :shopId AND a.status = 'ACTIVE'")
    long countActiveByShopId(UUID shopId);
}
