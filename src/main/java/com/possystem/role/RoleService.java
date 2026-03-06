package com.possystem.role;

import com.possystem.auth.user.UserRepository;
import com.possystem.common.ListResponse;
import com.possystem.common.UserType;
import com.possystem.permission.Permission;
import com.possystem.permission.PermissionAction;
import com.possystem.permission.PermissionRepository;
import com.possystem.permission.PermissionResponse;
import com.possystem.security.SecurityContextUtil;
import com.possystem.security.UserPrincipal;
import com.possystem.shop.UserShopAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final UserShopAssignmentRepository userShopAssignmentRepository;
    private final ModelMapper modelMapper;

    // ==================== CRUD ====================

    @Transactional
    public RoleResponse save(RoleRequest request) {
        UserPrincipal principal = SecurityContextUtil.getCurrentPrincipal();
        UUID tenantId = principal.getTenantId();
        UUID shopId = principal.getShopId();
        UserType userType = principal.getUserType();

        if (request.getId() != null) {
            return updateRole(request, tenantId, shopId, userType);
        }
        return createRole(request, tenantId, shopId, userType);
    }

    private RoleResponse createRole(RoleRequest request, UUID tenantId, UUID shopId, UserType userType) {
        // SHOP_MANAGER creates shop-scoped roles
        // TENANT_ADMIN creates tenant-level roles (or shop-scoped if shopId provided in request)
        UUID targetShopId = resolveTargetShopId(request, shopId, userType);

        // Validate unique name
        if (targetShopId != null) {
            if (roleRepository.existsByShopIdAndRoleNameIgnoreCase(targetShopId, request.getRoleName())) {
                throw new IllegalArgumentException("A role with this name already exists in this shop");
            }
        } else {
            if (roleRepository.existsByTenantIdAndRoleNameIgnoreCase(tenantId, request.getRoleName())) {
                throw new IllegalArgumentException("A role with this name already exists");
            }
        }

        validatePermissionIds(request.getPermissionIds());

        Role role = Role.builder()
                .roleCode(generateRoleCode())
                .roleName(request.getRoleName())
                .roleType(RoleType.CUSTOM)
                .tenantId(tenantId)
                .shopId(targetShopId)
                .description(request.getDescription())
                .isActive(true)
                .build();

        Role savedRole = roleRepository.save(role);
        saveRolePermissions(savedRole.getId(), request.getPermissionIds());

        return buildRoleResponse(savedRole);
    }

    private RoleResponse updateRole(RoleRequest request, UUID tenantId, UUID shopId, UserType userType) {
        Role role = roleRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        if (role.getRoleType() == RoleType.SYSTEM) {
            // SYSTEM roles can only be edited by TENANT_ADMIN (permissions only, not name)
            if (userType != UserType.TENANT_ADMIN && userType != UserType.SYSTEM_OWNER) {
                throw new IllegalArgumentException("Only tenant admin can modify default roles");
            }
        }

        // Validate access
        validateRoleAccess(role, tenantId, shopId, userType);

        // Check if name changed and is unique
        if (!role.getRoleName().equalsIgnoreCase(request.getRoleName())) {
            if (role.getShopId() != null) {
                if (roleRepository.existsByShopIdAndRoleNameIgnoreCase(role.getShopId(), request.getRoleName())) {
                    throw new IllegalArgumentException("A role with this name already exists in this shop");
                }
            } else {
                if (roleRepository.existsByTenantIdAndRoleNameIgnoreCase(tenantId, request.getRoleName())) {
                    throw new IllegalArgumentException("A role with this name already exists");
                }
            }
        }

        validatePermissionIds(request.getPermissionIds());

        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        Role savedRole = roleRepository.save(role);

        rolePermissionRepository.deleteByRoleId(savedRole.getId());
        saveRolePermissions(savedRole.getId(), request.getPermissionIds());

        return buildRoleResponse(savedRole);
    }

    public ListResponse<RoleResponse> fetch(RoleFetchRequest request) {
        UserPrincipal principal = SecurityContextUtil.getCurrentPrincipal();
        UUID tenantId = principal.getTenantId();
        UUID shopId = principal.getShopId();
        UserType userType = principal.getUserType();

        // Fetch by id
        if (request.getId() != null) {
            Role role = roleRepository.findById(request.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found"));
            validateRoleAccess(role, tenantId, shopId, userType);
            return ListResponse.of(List.of(buildRoleResponse(role)));
        }

        String search = request.getSearch();
        Integer limit = request.getLimit();

        // TENANT_ADMIN can pass shopId to view a specific shop's roles
        UUID targetShopId = shopId;
        if ((userType == UserType.TENANT_ADMIN || userType == UserType.SYSTEM_OWNER)
                && request.getShopId() != null) {
            targetShopId = request.getShopId();
        }

        // Shop-scoped view: shop roles + tenant-level roles
        if (targetShopId != null) {
            if (limit == null) {
                List<Role> all = roleRepository.searchByShopAndTenant(targetShopId, tenantId, search);
                return ListResponse.of(all.stream().map(this::buildRoleResponse).toList());
            }
            PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
            Page<Role> page = roleRepository.searchByShopAndTenant(targetShopId, tenantId, search, pageRequest);
            return ListResponse.from(page.map(this::buildRoleResponse));
        }

        // TENANT_ADMIN with no shopId filter sees tenant-level roles only
        if (limit == null) {
            List<Role> all = roleRepository.searchAll(tenantId, search);
            return ListResponse.of(all.stream().map(this::buildRoleResponse).toList());
        }
        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<Role> page = roleRepository.searchAll(tenantId, search, pageRequest);
        return ListResponse.from(page.map(this::buildRoleResponse));
    }

    @Transactional
    public void delete(UUID id) {
        UserPrincipal principal = SecurityContextUtil.getCurrentPrincipal();
        UUID tenantId = principal.getTenantId();
        UUID shopId = principal.getShopId();
        UserType userType = principal.getUserType();

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        validateRoleAccess(role, tenantId, shopId, userType);

        if (role.getRoleType() == RoleType.SYSTEM) {
            throw new IllegalArgumentException("Default roles cannot be deleted");
        }

        long userCount = userRepository.countByRoleId(id);
        if (userCount > 0) {
            throw new IllegalArgumentException("Role is assigned to " + userCount + " user(s) and cannot be deleted");
        }

        // Also check shop assignments
        long assignmentCount = userShopAssignmentRepository.countByRoleId(id);
        if (assignmentCount > 0) {
            throw new IllegalArgumentException("Role is assigned to " + assignmentCount + " shop user(s) and cannot be deleted");
        }

        rolePermissionRepository.deleteByRoleId(id);
        roleRepository.deleteById(id);
    }

    // ==================== DEFAULT ROLES ====================

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

    /**
     * Creates default shop roles when a new shop is created.
     * Copies permissions from tenant-level template roles (Admin → Shop Manager, Viewer → Shop User).
     * Returns the Shop Manager role ID for assignment.
     */
    @Transactional
    public UUID createDefaultShopRoles(UUID tenantId, UUID shopId) {
        // Get tenant-level Admin role as template for Shop Manager
        Role adminTemplate = roleRepository
                .findByTenantIdAndRoleNameIgnoreCaseAndRoleType(tenantId, "Admin", RoleType.SYSTEM)
                .orElse(null);

        // Get tenant-level Viewer role as template for Shop User
        Role viewerTemplate = roleRepository
                .findByTenantIdAndRoleNameIgnoreCaseAndRoleType(tenantId, "Viewer", RoleType.SYSTEM)
                .orElse(null);

        // Create Shop Manager role — copies from Admin template
        Role shopManagerRole = Role.builder()
                .roleCode(generateRoleCode())
                .roleName("Shop Manager")
                .roleType(RoleType.SYSTEM)
                .tenantId(tenantId)
                .shopId(shopId)
                .description("Full shop management access")
                .isActive(true)
                .build();
        Role savedManagerRole = roleRepository.save(shopManagerRole);

        if (adminTemplate != null) {
            List<UUID> adminPermIds = rolePermissionRepository.findByRoleId(adminTemplate.getId()).stream()
                    .map(RolePermission::getPermissionId)
                    .toList();
            saveRolePermissions(savedManagerRole.getId(), adminPermIds);
        } else {
            // Fallback: all permissions
            List<UUID> allPermIds = permissionRepository.findAll().stream()
                    .map(Permission::getId)
                    .toList();
            saveRolePermissions(savedManagerRole.getId(), allPermIds);
        }

        // Create Shop User role — copies from Viewer template
        Role shopUserRole = Role.builder()
                .roleCode(generateRoleCode())
                .roleName("Shop User")
                .roleType(RoleType.SYSTEM)
                .tenantId(tenantId)
                .shopId(shopId)
                .description("Basic shop access with view permissions")
                .isActive(true)
                .build();
        Role savedUserRole = roleRepository.save(shopUserRole);

        if (viewerTemplate != null) {
            List<UUID> viewerPermIds = rolePermissionRepository.findByRoleId(viewerTemplate.getId()).stream()
                    .map(RolePermission::getPermissionId)
                    .toList();
            saveRolePermissions(savedUserRole.getId(), viewerPermIds);
        } else {
            // Fallback: VIEW-only permissions
            List<UUID> viewPermIds = permissionRepository.findAll().stream()
                    .filter(p -> p.getAction() == PermissionAction.VIEW)
                    .map(Permission::getId)
                    .toList();
            saveRolePermissions(savedUserRole.getId(), viewPermIds);
        }

        return savedManagerRole.getId();
    }

    public UUID getAdminRoleId(UUID tenantId) {
        return roleRepository.findByTenantIdAndRoleNameIgnoreCaseAndRoleType(tenantId, "Admin", RoleType.SYSTEM)
                .map(Role::getId)
                .orElse(null);
    }

    public UUID getShopManagerRoleId(UUID shopId) {
        return roleRepository.findByShopIdAndRoleNameIgnoreCaseAndRoleType(shopId, "Shop Manager", RoleType.SYSTEM)
                .map(Role::getId)
                .orElse(null);
    }

    // ==================== HELPERS ====================

    private UUID resolveTargetShopId(RoleRequest request, UUID currentShopId, UserType userType) {
        if (userType == UserType.SHOP_MANAGER || userType == UserType.SHOP_USER) {
            if (currentShopId == null) {
                throw new IllegalArgumentException("You are not assigned to a shop");
            }
            return currentShopId;
        }
        // TENANT_ADMIN can optionally create shop-scoped roles by passing shopId
        return request.getShopId();
    }

    private void validateRoleAccess(Role role, UUID tenantId, UUID shopId, UserType userType) {
        if (!role.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Role not found");
        }

        // SHOP_MANAGER can only access roles scoped to their shop or tenant-level roles (read-only)
        if (userType == UserType.SHOP_MANAGER || userType == UserType.SHOP_USER) {
            if (role.getShopId() != null && !role.getShopId().equals(shopId)) {
                throw new IllegalArgumentException("Role not found");
            }
        }
    }

    private void saveRolePermissions(UUID roleId, List<UUID> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) return;
        List<RolePermission> rolePermissions = permissionIds.stream()
                .map(permId -> RolePermission.builder()
                        .roleId(roleId)
                        .permissionId(permId)
                        .build())
                .toList();
        rolePermissionRepository.saveAll(rolePermissions);
    }

    private void validatePermissionIds(List<UUID> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) return;
        long count = permissionRepository.countByIdIn(permissionIds);
        if (count != permissionIds.size()) {
            throw new IllegalArgumentException("One or more permissions not found");
        }
    }

    private RoleResponse buildRoleResponse(Role role) {
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
        long assignmentCount = userShopAssignmentRepository.countByRoleId(role.getId());

        return RoleResponse.builder()
                .id(role.getId())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .roleType(role.getRoleType())
                .shopId(role.getShopId())
                .description(role.getDescription())
                .isActive(role.getIsActive())
                .createdAt(role.getCreatedAt())
                .permissions(permissions)
                .userCount(userCount + assignmentCount)
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
}
