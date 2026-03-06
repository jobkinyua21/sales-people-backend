package com.possystem.mpesa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MpesaTransactionRepository extends JpaRepository<MpesaTransaction, UUID> {

    // Global lookup — used by M-Pesa callback (external webhook, no auth context)
    Optional<MpesaTransaction> findByCheckoutRequestId(String checkoutRequestId);

    Optional<MpesaTransaction> findByOrderIdAndStatus(UUID orderId, MpesaTransactionStatus status);

    Optional<MpesaTransaction> findTopByOrderIdOrderByCreatedAtDesc(UUID orderId);

    // Shop-scoped lookups — used by authenticated status check endpoints
    Optional<MpesaTransaction> findByCheckoutRequestIdAndShopId(String checkoutRequestId, UUID shopId);

    Optional<MpesaTransaction> findTopByOrderIdAndShopIdOrderByCreatedAtDesc(UUID orderId, UUID shopId);
}
