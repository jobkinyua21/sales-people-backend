package com.salespeople.role;

import com.salespeople.auth.user.UserRepository;
import com.salespeople.common.ListResponse;
import com.salespeople.common.UserType;
import com.salespeople.permission.Permission;
import com.salespeople.permission.PermissionAction;
import com.salespeople.permission.PermissionRepository;
import com.salespeople.permission.PermissionResponse;
import com.salespeople.security.SecurityContextUtil;
import com.salespeople.security.UserPrincipal;
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
    private final ModelMapper modelMapper;

    // ==================== CRUD ====================

    @Transactional
    public RoleResponse save(RoleRequest request) {
        if (request.getId() != null) {
            return updateRole(request);
        }
        return createRole(request);
    }

    private RoleResponse createRole(RoleRequest request) {
        if (roleRepository.existsByRoleNameIgnoreCase(request.getRoleName())) {
            throw new IllegalArgumentException("A role with this name already exists");
        }

        validatePermissionIds(request.getPermissionIds());

        Role role = Role.builder()
                .roleCode(generateRoleCode())
                .roleName(request.getRoleName())
                .roleType(RoleType.CUSTOM)
                .description(request.getDescription())
                .isActive(true)
                .build();

        Role savedRole = roleRepository.save(role);
        saveRolePermissions(savedRole.getId(), request.getPermissionIds());

        return buildRoleResponse(savedRole);
    }

    private RoleResponse updateRole(RoleRequest request) {
        Role role = roleRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        if (role.getRoleType() == RoleType.SYSTEM) {
            UserPrincipal principal = SecurityContextUtil.getCurrentPrincipal();
            if (principal.getUserType() != UserType.ADMIN) {
                throw new IllegalArgumentException("Only admin can modify default roles");
            }
        }

        if (!role.getRoleName().equalsIgnoreCase(request.getRoleName())) {
            if (roleRepository.existsByRoleNameIgnoreCase(request.getRoleName())) {
                throw new IllegalArgumentException("A role with this name already exists");
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
        if (request.getId() != null) {
            Role role = roleRepository.findById(request.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found"));
            return ListResponse.of(List.of(buildRoleResponse(role)));
        }

        String search = request.getSearch();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<Role> all = roleRepository.searchAll(search);
            return ListResponse.of(all.stream().map(this::buildRoleResponse).toList());
        }
        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<Role> page = roleRepository.searchAll(search, pageRequest);
        return ListResponse.from(page.map(this::buildRoleResponse));
    }

    @Transactional
    public void delete(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        if (role.getRoleType() == RoleType.SYSTEM) {
            throw new IllegalArgumentException("Default roles cannot be deleted");
        }

        long userCount = userRepository.countByRoleId(id);
        if (userCount > 0) {
            throw new IllegalArgumentException("Role is assigned to " + userCount + " user(s) and cannot be deleted");
        }

        rolePermissionRepository.deleteByRoleId(id);
        roleRepository.deleteById(id);
    }

    // ==================== DEFAULT ROLES ====================

    @Transactional
    public void createSystemRoles() {
        List<Permission> allPermissions = permissionRepository.findAll();

        // Create Admin role with ALL permissions
        if (roleRepository.findByRoleNameIgnoreCaseAndRoleType("Admin", RoleType.SYSTEM).isEmpty()) {
            Role adminRole = Role.builder()
                    .roleCode(generateRoleCode())
                    .roleName("Admin")
                    .roleType(RoleType.SYSTEM)
                    .description("Full access to all modules")
                    .isActive(true)
                    .build();
            Role savedAdmin = roleRepository.save(adminRole);

            List<UUID> allPermissionIds = allPermissions.stream()
                    .map(Permission::getId)
                    .toList();
            saveRolePermissions(savedAdmin.getId(), allPermissionIds);
        }

        // Create Sales Person role with VIEW-only permissions
        if (roleRepository.findByRoleNameIgnoreCaseAndRoleType("Sales Person", RoleType.SYSTEM).isEmpty()) {
            Role salesRole = Role.builder()
                    .roleCode(generateRoleCode())
                    .roleName("Sales Person")
                    .roleType(RoleType.SYSTEM)
                    .description("Sales order access")
                    .isActive(true)
                    .build();
            Role savedSales = roleRepository.save(salesRole);

            List<UUID> viewPermissionIds = allPermissions.stream()
                    .filter(p -> p.getAction() == PermissionAction.VIEW)
                    .map(Permission::getId)
                    .toList();
            saveRolePermissions(savedSales.getId(), viewPermissionIds);
        }
    }

    public UUID getAdminRoleId() {
        return roleRepository.findByRoleNameIgnoreCaseAndRoleType("Admin", RoleType.SYSTEM)
                .map(Role::getId)
                .orElse(null);
    }

    public UUID getSalesPersonRoleId() {
        return roleRepository.findByRoleNameIgnoreCaseAndRoleType("Sales Person", RoleType.SYSTEM)
                .map(Role::getId)
                .orElse(null);
    }

    // ==================== HELPERS ====================

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
}
