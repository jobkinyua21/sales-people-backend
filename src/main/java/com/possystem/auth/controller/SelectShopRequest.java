package com.possystem.auth.controller;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelectShopRequest {

    @NotNull(message = "User ID is required")
    private UUID usrId;

    @NotNull(message = "Shop ID is required")
    private UUID shopId;
}
