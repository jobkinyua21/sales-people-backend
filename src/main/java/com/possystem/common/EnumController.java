package com.possystem.common;

import com.possystem.module.enums.ModuleStatus;
import com.possystem.permission.PermissionAction;
import com.possystem.role.RoleType;
import com.possystem.shop.enums.BillingCycle;
import com.possystem.shop.enums.PaymentMode;
import com.possystem.shop.enums.ShopStatus;
import com.possystem.shop.enums.SubscriptionStatus;
import com.possystem.subscription.SubscriptionPlanStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/v1/enums")
@RequiredArgsConstructor
public class EnumController {

    @PostMapping("/fetch")
    public ResponseEntity<Map<String, List<String>>> fetchAll() {
        Map<String, List<String>> enums = new LinkedHashMap<>();
        enums.put("shopStatus", enumValues(ShopStatus.class));
        enums.put("subscriptionStatus", enumValues(SubscriptionStatus.class));
        enums.put("billingCycle", enumValues(BillingCycle.class));
        enums.put("paymentMode", enumValues(PaymentMode.class));
        enums.put("subscriptionPlanStatus", enumValues(SubscriptionPlanStatus.class));
        enums.put("userType", enumValues(UserType.class));
        enums.put("userStatus", enumValues(UserStatus.class));
        enums.put("moduleStatus", enumValues(ModuleStatus.class));
        enums.put("roleType", enumValues(RoleType.class));
        enums.put("permissionAction", enumValues(PermissionAction.class));
        return ResponseEntity.ok(enums);
    }

    private <E extends Enum<E>> List<String> enumValues(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .toList();
    }
}
