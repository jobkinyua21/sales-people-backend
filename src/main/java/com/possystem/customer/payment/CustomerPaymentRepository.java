package com.possystem.customer.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerPaymentRepository extends JpaRepository<CustomerPayment, UUID> {

    long countByShopId(UUID shopId);

    boolean existsByShopIdAndReceiptNumber(UUID shopId, String receiptNumber);

    @Query("SELECT cp FROM CustomerPayment cp WHERE cp.shopId = :shopId AND cp.customerId = :customerId " +
            "AND cp.isActive = true ORDER BY cp.createdAt DESC")
    List<CustomerPayment> findByCustomer(@Param("shopId") UUID shopId, @Param("customerId") UUID customerId);
}
