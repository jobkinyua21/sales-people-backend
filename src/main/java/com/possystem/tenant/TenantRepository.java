package com.possystem.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByUsrId(UUID usrId);

    Optional<Tenant> findByBusinessName(String businessName);

    Optional<Tenant> findByBusinessRegistrationNumber(String businessRegistrationNumber);

    boolean existsByUsrId(UUID usrId);

    boolean existsByBusinessName(String businessName);

    boolean existsByBusinessRegistrationNumber(String businessRegistrationNumber);

    Optional<Tenant> findByTenantCode(String tenantCode);

    boolean existsByTenantCode(String tenantCode);
}
