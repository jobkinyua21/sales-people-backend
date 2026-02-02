package com.possystem.auth.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request for POS users (shop staff)")
public class PosLoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "User's email or phone number", example = "staff@shop.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "User's password", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "Shop code is required")
    @Schema(description = "Unique shop code (memorable identifier)", example = "SHOP001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String shopCode;
}
