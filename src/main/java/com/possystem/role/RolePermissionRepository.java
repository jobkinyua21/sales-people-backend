package com.possystem.role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    List<RolePermission> findByRoleId(UUID roleId);

    void deleteByRoleId(UUID roleId);

    @Query("SELECT p.permissionCode FROM RolePermission rp " +
            "JOIN com.possystem.permission.Permission p ON p.id = rp.permissionId " +
            "WHERE rp.roleId = :roleId")
    List<String> findPermissionCodesByRoleId(@Param("roleId") UUID roleId);
}
