package com.possystem.auth.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing profile")
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Profile name must be between 2 and 100 characters")
    @Schema(description = "Profile name", example = "Senior Manager")
    private String profileName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Profile description")
    private String description;

    @Schema(description = "Profile active status")
    private Boolean isActive;
}
