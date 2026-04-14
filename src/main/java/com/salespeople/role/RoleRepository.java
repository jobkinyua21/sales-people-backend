package com.salespeople.role;

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

    boolean existsByRoleNameIgnoreCase(String roleName);

    Optional<Role> findByRoleNameIgnoreCaseAndRoleType(String roleName, RoleType roleType);

    @Query("SELECT r FROM Role r WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(r.roleName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.roleCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(r.roleType AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY r.createdAt DESC")
    Page<Role> searchAll(@Param("search") String search, Pageable pageable);

    @Query("SELECT r FROM Role r WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(r.roleName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.roleCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(r.roleType AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY r.createdAt DESC")
    List<Role> searchAll(@Param("search") String search);
}
