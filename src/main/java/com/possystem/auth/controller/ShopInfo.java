package com.possystem.auth.controller;

import com.possystem.common.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopInfo {

    private UUID shopId;

    private String shopName;

    private String shopCode;

    private UserType shopRole;
}
