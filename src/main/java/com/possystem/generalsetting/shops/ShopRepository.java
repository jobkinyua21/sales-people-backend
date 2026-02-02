package com.possystem.generalsetting.shops;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID> {

    List<Shop> findByTenantId(UUID tenantId);

    Optional<Shop> findByShopCode(String shopCode);

    Optional<Shop> findByShopIdAndTenantId(UUID shopId, UUID tenantId);

    boolean existsByShopCode(String shopCode);

    List<Shop> findByTenantIdAndIsActiveTrue(UUID tenantId);
}
