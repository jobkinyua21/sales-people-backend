package com.possystem.generalsetting.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopUserRepository extends JpaRepository<ShopUser, UUID> {

    List<ShopUser> findByTenantId(UUID tenantId);

    List<ShopUser> findByShopId(UUID shopId);

    List<ShopUser> findByTenantIdAndShopId(UUID tenantId, UUID shopId);

    Optional<ShopUser> findByEmail(String email);

    Optional<ShopUser> findByPhoneNumber(String phoneNumber);

    Optional<ShopUser> findByEmailOrPhoneNumber(String email, String phoneNumber);

    Optional<ShopUser> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    Optional<ShopUser> findByUserIdAndShopId(UUID userId, UUID shopId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    List<ShopUser> findByShopIdAndIsActiveTrue(UUID shopId);
}
