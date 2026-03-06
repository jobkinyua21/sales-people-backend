package com.possystem.security;

import com.possystem.businesstype.BusinessTypeModuleRepository;
import com.possystem.shop.Shop;
import com.possystem.shop.ShopAdditionalModuleRepository;
import com.possystem.shop.ShopRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ModuleAccessInterceptor implements HandlerInterceptor {

    private final ShopAdditionalModuleRepository shopAdditionalModuleRepository;
    private final BusinessTypeModuleRepository businessTypeModuleRepository;
    private final ShopRepository shopRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiresModule annotation = handlerMethod.getMethodAnnotation(RequiresModule.class);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType().getAnnotation(RequiresModule.class);
        }
        if (annotation == null) {
            return true;
        }

        String requiredModuleCode = annotation.value();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return true; // Let Spring Security handle unauthenticated requests
        }

        // SYSTEM_OWNER and TENANT_ADMIN bypass module checks
        if (principal.getUserType().isTenantLevel()) {
            return true;
        }

        UUID shopId = principal.getShopId();
        if (shopId == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"errorCode\":\"MODULE_ACCESS_DENIED\"," +
                    "\"message\":\"Shop context is required\"}");
            return false;
        }

        if (hasModuleAccess(shopId, requiredModuleCode)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"success\":false,\"errorCode\":\"MODULE_ACCESS_DENIED\"," +
                "\"message\":\"Your shop does not have access to the " + requiredModuleCode + " module\"}");
        return false;
    }

    private boolean hasModuleAccess(UUID shopId, String moduleCode) {
        // Check explicitly subscribed modules
        if (shopAdditionalModuleRepository.existsByShopIdAndModuleCode(shopId, moduleCode)) {
            return true;
        }
        // Check default modules from business type
        Shop shop = shopRepository.findById(shopId).orElse(null);
        if (shop != null && shop.getBusinessTypeId() != null) {
            return businessTypeModuleRepository.existsDefaultModuleByBusinessTypeIdAndModuleCode(
                    shop.getBusinessTypeId(), moduleCode);
        }
        return false;
    }
}
