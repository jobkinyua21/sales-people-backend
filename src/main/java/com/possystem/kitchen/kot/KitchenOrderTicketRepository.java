package com.possystem.kitchen.kot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KitchenOrderTicketRepository extends JpaRepository<KitchenOrderTicket, UUID> {

    Optional<KitchenOrderTicket> findByIdAndShopId(UUID id, UUID shopId);

    List<KitchenOrderTicket> findAllByShopIdAndStatusOrderByPriorityAscCreatedAtAsc(UUID shopId, KotStatus status);

    List<KitchenOrderTicket> findAllByShopIdAndStatusInOrderByPriorityAscCreatedAtAsc(UUID shopId, List<KotStatus> statuses);

    List<KitchenOrderTicket> findAllByOrderIdAndShopId(UUID orderId, UUID shopId);

    List<KitchenOrderTicket> findAllByShopIdOrderByCreatedAtDesc(UUID shopId);

    long countByShopId(UUID shopId);

    long countByShopIdAndStatus(UUID shopId, KotStatus status);
}
