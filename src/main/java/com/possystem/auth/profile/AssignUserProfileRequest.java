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
@Schema(description = "Request to assign a profile to a user")
public class AssignUserProfileRequest {

    @NotNull(message = "User ID is required")
    @Schema(description = "User ID to assign profile to")
    private UUID userId;

    @NotNull(message = "Profile ID is required")
    @Schema(description = "Profile ID to assign")
    private UUID profileId;
}
