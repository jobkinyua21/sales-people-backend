package com.possystem.auth.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Permission response")
public class PermissionResponse {

    @Schema(description = "Permission ID")
    private UUID permissionId;

    @Schema(description = "Permission name")
    private String permissionName;

    @Schema(description = "Permission code")
    private String permissionCode;

    @Schema(description = "Permission description")
    private String description;

    @Schema(description = "Is permission active")
    private Boolean isActive;
}
