package com.salespeople.security;

import com.salespeople.common.UserType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class ModuleAccessInterceptor implements HandlerInterceptor {

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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return true;
        }

        // Admin bypasses module checks
        if (principal.getUserType() == UserType.ADMIN) {
            return true;
        }

        // For now, allow all authenticated users - module access can be extended later
        return true;
    }
}
