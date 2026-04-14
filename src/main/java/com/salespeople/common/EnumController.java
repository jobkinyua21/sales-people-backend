package com.salespeople.common;

import com.salespeople.permission.PermissionAction;
import com.salespeople.role.RoleType;
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
        enums.put("userType", enumValues(UserType.class));
        enums.put("userStatus", enumValues(UserStatus.class));
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
