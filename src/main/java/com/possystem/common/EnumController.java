package com.possystem.common;

import com.possystem.customer.CustomerGender;
import com.possystem.customer.CustomerStatus;
import com.possystem.inventory.ProductStatus;
import com.possystem.supplier.PaymentTerms;
import com.possystem.sales.DiscountType;
import com.possystem.sales.OrderStatus;
import com.possystem.sales.PaymentMethod;
import com.possystem.sales.PaymentStatus;
import com.possystem.purchasing.enums.GrnStatus;
import com.possystem.purchasing.enums.PurchaseOrderStatus;
import com.possystem.supplier.SupplierStatus;
import com.possystem.inventory.ProductType;
import com.possystem.inventory.ProductVariantStatus;
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
        enums.put("productType", enumValues(ProductType.class));
        enums.put("productStatus", enumValues(ProductStatus.class));
        enums.put("productVariantStatus", enumValues(ProductVariantStatus.class));
        enums.put("stockStatus", List.of("IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"));
        enums.put("customerStatus", enumValues(CustomerStatus.class));
        enums.put("customerGender", enumValues(CustomerGender.class));
        enums.put("supplierStatus", enumValues(SupplierStatus.class));
        enums.put("paymentTerms", enumValues(PaymentTerms.class));
        enums.put("orderStatus", enumValues(OrderStatus.class));
        enums.put("paymentMethod", enumValues(PaymentMethod.class));
        enums.put("paymentStatus", enumValues(PaymentStatus.class));
        enums.put("discountType", enumValues(DiscountType.class));
        enums.put("purchaseOrderStatus", enumValues(PurchaseOrderStatus.class));
        enums.put("grnStatus", enumValues(GrnStatus.class));
        return ResponseEntity.ok(enums);
    }

    private <E extends Enum<E>> List<String> enumValues(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .toList();
    }
}
