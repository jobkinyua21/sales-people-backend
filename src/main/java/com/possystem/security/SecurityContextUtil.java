package com.possystem.security;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    public static UserPrincipal getCurrentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return (UserPrincipal) authentication.getPrincipal();
    }

    public static UUID getCurrentShopId() {
        UserPrincipal principal = getCurrentPrincipal();
        UUID shopId = principal.getShopId();
        if (shopId == null) {
            throw new IllegalArgumentException("Shop context is required");
        }
        return shopId;
    }

    public static UUID getCurrentTenantId() {
        return getCurrentPrincipal().getTenantId();
    }

    public static UUID getCurrentUserId() {
        return getCurrentPrincipal().getId();
    }
}
