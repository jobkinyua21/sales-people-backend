package com.possystem.role;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    boolean existsByRoleCode(String roleCode);

    boolean existsByTenantIdAndRoleNameIgnoreCase(UUID tenantId, String roleName);

    Optional<Role> findByTenantIdAndRoleNameIgnoreCaseAndRoleType(UUID tenantId, String roleName, RoleType roleType);

    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(r.roleName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.roleCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(r.roleType AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY r.createdAt DESC")
    Page<Role> searchAll(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(r.roleName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.roleCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(r.roleType AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY r.createdAt DESC")
    List<Role> searchAll(@Param("tenantId") UUID tenantId, @Param("search") String search);
}
