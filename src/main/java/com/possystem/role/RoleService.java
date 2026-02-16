package com.possystem.role;

import com.possystem.auth.user.UserRepository;
import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import com.possystem.permission.Permission;
import com.possystem.permission.PermissionAction;
import com.possystem.permission.PermissionRepository;
import com.possystem.permission.PermissionResponse;
import com.possystem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public RoleResponse save(RoleRequest request) {
        UUID tenantId = getCurrentTenantId();

        if (request.getId() != null) {
            return updateRole(request, tenantId);
        }
        return createRole(request, tenantId);
    }

    private RoleResponse createRole(RoleRequest request, UUID tenantId) {
        // Validate unique name per tenant
        if (roleRepository.existsByTenantIdAndRoleNameIgnoreCase(tenantId, request.getRoleName())) {
            throw new IllegalArgumentException("A role with this name already exists");
        }

        // Validate all permission IDs exist
        validatePermissionIds(request.getPermissionIds());

        Role role = Role.builder()
                .roleCode(generateRoleCode())
                .roleName(request.getRoleName())
                .roleType(RoleType.CUSTOM)
                .tenantId(tenantId)
                .description(request.getDescription())
                .isActive(true)
                .build();

        Role savedRole = roleRepository.save(role);

        // Save permissions
        saveRolePermissions(savedRole.getId(), request.getPermissionIds());

        return buildRoleResponse(savedRole);
    }

    private RoleResponse updateRole(RoleRequest request, UUID tenantId) {
        Role role = roleRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        // Only CUSTOM roles can be edited
        if (role.getRoleType() == RoleType.SYSTEM) {
            throw new IllegalArgumentException("System roles cannot be modified");
        }

        // Validate tenant ownership
        if (!role.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Role not found");
        }

        // Check if name changed and is unique
        if (!role.getRoleName().equalsIgnoreCase(request.getRoleName())
                && roleRepository.existsByTenantIdAndRoleNameIgnoreCase(tenantId, request.getRoleName())) {
            throw new IllegalArgumentException("A role with this name already exists");
        }

        // Validate all permission IDs exist
        validatePermissionIds(request.getPermissionIds());

        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        Role savedRole = roleRepository.save(role);

        // Sync permissions: delete old, insert new
        rolePermissionRepository.deleteByRoleId(savedRole.getId());
        saveRolePermissions(savedRole.getId(), request.getPermissionIds());

        return buildRoleResponse(savedRole);
    }

    public ListResponse<RoleResponse> fetch(FetchRequest request) {
        UUID tenantId = getCurrentTenantId();

        // Fetch by id
        if (request.getId() != null) {
            Role role = roleRepository.findById(request.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found"));
            if (!role.getTenantId().equals(tenantId)) {
                throw new IllegalArgumentException("Role not found");
            }
            return ListResponse.of(List.of(buildRoleResponse(role)));
        }

        String search = request.getSearch();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<Role> all = roleRepository.searchAll(tenantId, search);
            List<RoleResponse> responses = all.stream()
                    .map(this::buildRoleResponse)
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<Role> page = roleRepository.searchAll(tenantId, search, pageRequest);
        Page<RoleResponse> responsePage = page.map(this::buildRoleResponse);
        return ListResponse.from(responsePage);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = getCurrentTenantId();

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        if (!role.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Role not found");
        }

        if (role.getRoleType() == RoleType.SYSTEM) {
            throw new IllegalArgumentException("System roles cannot be deleted");
        }

        // Check if any users are assigned to this role
        long userCount = userRepository.countByRoleId(id);
        if (userCount > 0) {
            throw new IllegalArgumentException("Role is assigned to " + userCount + " user(s) and cannot be deleted");
        }

        rolePermissionRepository.deleteByRoleId(id);
        roleRepository.deleteById(id);
    }

    @Transactional
    public void createSystemRolesForTenant(UUID tenantId) {
        List<Permission> allPermissions = permissionRepository.findAll();

        // Create Admin role with ALL permissions
        Role adminRole = Role.builder()
                .roleCode(generateRoleCode())
                .roleName("Admin")
                .roleType(RoleType.SYSTEM)
                .tenantId(tenantId)
                .description("Full access to all modules")
                .isActive(true)
                .build();
        Role savedAdmin = roleRepository.save(adminRole);

        List<UUID> allPermissionIds = allPermissions.stream()
                .map(Permission::getId)
                .toList();
        saveRolePermissions(savedAdmin.getId(), allPermissionIds);

        // Create Viewer role with VIEW-only permissions
        Role viewerRole = Role.builder()
                .roleCode(generateRoleCode())
                .roleName("Viewer")
                .roleType(RoleType.SYSTEM)
                .tenantId(tenantId)
                .description("View-only access across all modules")
                .isActive(true)
                .build();
        Role savedViewer = roleRepository.save(viewerRole);

        List<UUID> viewPermissionIds = allPermissions.stream()
                .filter(p -> p.getAction() == PermissionAction.VIEW)
                .map(Permission::getId)
                .toList();
        saveRolePermissions(savedViewer.getId(), viewPermissionIds);
    }

    public UUID getAdminRoleId(UUID tenantId) {
        return roleRepository.findByTenantIdAndRoleNameIgnoreCaseAndRoleType(tenantId, "Admin", RoleType.SYSTEM)
                .map(Role::getId)
                .orElse(null);
    }

    // ==================== HELPERS ====================

    private void saveRolePermissions(UUID roleId, List<UUID> permissionIds) {
        List<RolePermission> rolePermissions = permissionIds.stream()
                .map(permId -> RolePermission.builder()
                        .roleId(roleId)
                        .permissionId(permId)
                        .build())
                .toList();
        rolePermissionRepository.saveAll(rolePermissions);
    }

    private void validatePermissionIds(List<UUID> permissionIds) {
        long count = permissionRepository.countByIdIn(permissionIds);
        if (count != permissionIds.size()) {
            throw new IllegalArgumentException("One or more permissions not found");
        }
    }

    private RoleResponse buildRoleResponse(Role role) {
        // Get permissions for this role
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(role.getId());
        List<UUID> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .toList();

        List<PermissionResponse> permissions = List.of();
        if (!permissionIds.isEmpty()) {
            permissions = permissionRepository.findAllById(permissionIds).stream()
                    .map(p -> modelMapper.map(p, PermissionResponse.class))
                    .toList();
        }

        long userCount = userRepository.countByRoleId(role.getId());

        return RoleResponse.builder()
                .id(role.getId())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .roleType(role.getRoleType())
                .description(role.getDescription())
                .isActive(role.getIsActive())
                .createdAt(role.getCreatedAt())
                .permissions(permissions)
                .userCount(userCount)
                .build();
    }

    private String generateRoleCode() {
        long count = roleRepository.count();
        String code;
        do {
            count++;
            code = String.format("ROL-%04d", count);
        } while (roleRepository.existsByRoleCode(code));
        return code;
    }

    private UUID getCurrentTenantId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getTenantId();
    }
}
