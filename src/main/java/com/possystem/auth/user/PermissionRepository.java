package com.possystem.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByPermissionCode(String permissionCode);

    Optional<Permission> findByPermissionName(String permissionName);

    boolean existsByPermissionCode(String permissionCode);
}
