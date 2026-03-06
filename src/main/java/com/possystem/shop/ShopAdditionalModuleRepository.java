package com.possystem.shop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopAdditionalModuleRepository extends JpaRepository<ShopAdditionalModule, UUID> {

    List<ShopAdditionalModule> findByShopIdAndIsActiveTrue(UUID shopId);

    Optional<ShopAdditionalModule> findByShopIdAndAdditionalModuleId(UUID shopId, UUID additionalModuleId);

    boolean existsByShopIdAndAdditionalModuleIdAndIsActiveTrue(UUID shopId, UUID additionalModuleId);

    @Query("SELECT COUNT(sam) > 0 FROM ShopAdditionalModule sam " +
           "JOIN com.possystem.module.AdditionalModule am ON sam.additionalModuleId = am.id " +
           "WHERE sam.shopId = :shopId AND am.moduleCode = :moduleCode AND sam.isActive = true")
    boolean existsByShopIdAndModuleCode(@Param("shopId") UUID shopId, @Param("moduleCode") String moduleCode);
}
