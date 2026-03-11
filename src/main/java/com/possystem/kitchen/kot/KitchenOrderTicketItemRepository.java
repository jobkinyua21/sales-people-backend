package com.possystem.kitchen.kot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KitchenOrderTicketItemRepository extends JpaRepository<KitchenOrderTicketItem, UUID> {

    Optional<KitchenOrderTicketItem> findByIdAndKitchenOrderTicketShopId(UUID id, UUID shopId);

    List<KitchenOrderTicketItem> findAllByKitchenOrderTicketShopIdAndStatusOrderByCreatedAtAsc(UUID shopId, KotItemStatus status);

    List<KitchenOrderTicketItem> findAllByKitchenOrderTicketShopIdAndPrepStationAndStatusInOrderByCreatedAtAsc(
            UUID shopId, com.possystem.kitchen.recipe.PrepStation prepStation, List<KotItemStatus> statuses);
}
