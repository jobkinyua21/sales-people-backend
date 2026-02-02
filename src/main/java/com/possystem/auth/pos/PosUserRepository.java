package com.possystem.auth.pos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PosUserRepository extends JpaRepository<PosUser, UUID> {

    Optional<PosUser> findByEmail(String email);

    Optional<PosUser> findByPhoneNumber(String phoneNumber);

    Optional<PosUser> findByEmailOrPhoneNumber(String email, String phoneNumber);

    Optional<PosUser> findByEmailAndTenantId(String email, UUID tenantId);

    Optional<PosUser> findByPhoneNumberAndTenantId(String phoneNumber, UUID tenantId);

    Optional<PosUser> findByEmailAndShopId(String email, UUID shopId);

    Optional<PosUser> findByPhoneNumberAndShopId(String phoneNumber, UUID shopId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}
