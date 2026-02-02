package com.possystem.auth.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Permission assignment for a profile")
public class ProfilePermissionRequest {

    @NotNull(message = "Permission ID is required")
    @Schema(description = "Permission ID")
    private UUID permissionId;

    @Schema(description = "Can read permission", example = "true")
    @Builder.Default
    private Boolean canRead = false;

    @Schema(description = "Can write/update permission", example = "true")
    @Builder.Default
    private Boolean canWrite = false;

    @Schema(description = "Can create permission", example = "true")
    @Builder.Default
    private Boolean canCreate = false;

    @Schema(description = "Can approve permission", example = "false")
    @Builder.Default
    private Boolean canApprove = false;

    @Schema(description = "Can export permission", example = "true")
    @Builder.Default
    private Boolean canExport = false;
}
