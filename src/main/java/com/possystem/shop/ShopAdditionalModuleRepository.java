package com.possystem.shop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopAdditionalModuleRepository extends JpaRepository<ShopAdditionalModule, UUID> {

    List<ShopAdditionalModule> findByShopIdAndIsActiveTrue(UUID shopId);

    Optional<ShopAdditionalModule> findByShopIdAndAdditionalModuleId(UUID shopId, UUID additionalModuleId);

    boolean existsByShopIdAndAdditionalModuleIdAndIsActiveTrue(UUID shopId, UUID additionalModuleId);
}
