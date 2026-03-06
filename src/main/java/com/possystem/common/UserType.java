package com.possystem.common;

public enum UserType {
    SYSTEM_OWNER,
    TENANT_ADMIN,
    SHOP_MANAGER,
    SHOP_USER;

    public boolean isTenantLevel() {
        return this == SYSTEM_OWNER || this == TENANT_ADMIN;
    }

    public boolean isShopLevel() {
        return this == SHOP_MANAGER || this == SHOP_USER;
    }
}
