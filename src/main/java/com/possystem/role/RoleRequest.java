package com.possystem.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequest {

    private UUID id;

    @NotBlank(message = "Role name is required")
    private String roleName;

    private String description;

    @NotEmpty(message = "At least one permission is required")
    private List<UUID> permissionIds;
}
