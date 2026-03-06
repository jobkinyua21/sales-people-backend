package com.possystem.sales.register;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashRegisterSessionRepository extends JpaRepository<CashRegisterSession, UUID> {

    Optional<CashRegisterSession> findByShopIdAndOpenedByAndStatus(UUID shopId, UUID openedBy, RegisterSessionStatus status);

    Optional<CashRegisterSession> findByIdAndShopId(UUID id, UUID shopId);

    boolean existsByShopIdAndOpenedByAndStatus(UUID shopId, UUID openedBy, RegisterSessionStatus status);

    @Query("SELECT s FROM CashRegisterSession s WHERE s.shopId = :shopId ORDER BY s.openedAt DESC")
    List<CashRegisterSession> findAllByShopIdOrderByOpenedAtDesc(UUID shopId);

    @Query("SELECT s FROM CashRegisterSession s WHERE s.shopId = :shopId AND s.openedBy = :userId ORDER BY s.openedAt DESC")
    List<CashRegisterSession> findAllByShopIdAndOpenedByOrderByOpenedAtDesc(UUID shopId, UUID userId);
}
