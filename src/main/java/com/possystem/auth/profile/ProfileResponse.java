package com.possystem.auth.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Profile response")
public class ProfileResponse {

    @Schema(description = "Profile ID")
    private UUID profileId;

    @Schema(description = "Profile name")
    private String profileName;

    @Schema(description = "Profile code")
    private String profileCode;

    @Schema(description = "Profile description")
    private String description;

    @Schema(description = "Is profile active")
    private Boolean isActive;

    @Schema(description = "List of permissions assigned to this profile")
    private List<PermissionDetail> permissions;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Updated timestamp")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionDetail {
        private UUID permissionId;
        private String permissionName;
        private String permissionCode;
        private Boolean canRead;
        private Boolean canWrite;
        private Boolean canCreate;
        private Boolean canApprove;
        private Boolean canExport;
    }
}
