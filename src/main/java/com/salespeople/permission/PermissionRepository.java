package com.salespeople.permission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    List<Permission> findAllByOrderByModuleAscActionAsc();

    List<Permission> findByPermissionCodeIn(List<String> permissionCodes);

    boolean existsByPermissionCode(String permissionCode);

    List<Permission> findByActionOrderByModuleAsc(PermissionAction action);

    long countByIdIn(List<UUID> ids);
}
