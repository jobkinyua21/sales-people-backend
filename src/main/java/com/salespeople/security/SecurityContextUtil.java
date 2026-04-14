package com.salespeople.security;

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

    public static UUID getCurrentUserId() {
        return getCurrentPrincipal().getId();
    }
}
