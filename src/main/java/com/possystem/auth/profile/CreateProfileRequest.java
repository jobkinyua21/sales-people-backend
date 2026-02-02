package com.possystem.auth.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new profile")
public class CreateProfileRequest {

    @NotBlank(message = "Profile name is required")
    @Size(min = 2, max = 100, message = "Profile name must be between 2 and 100 characters")
    @Schema(description = "Profile name", example = "Manager")
    private String profileName;

    @NotBlank(message = "Profile code is required")
    @Size(min = 2, max = 100, message = "Profile code must be between 2 and 100 characters")
    @Schema(description = "Profile code (unique identifier)", example = "MANAGER")
    private String profileCode;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Profile description", example = "Manager role with full access")
    private String description;
}
