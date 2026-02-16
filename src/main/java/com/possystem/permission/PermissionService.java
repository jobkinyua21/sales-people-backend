package com.possystem.permission;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final ModelMapper modelMapper;

    public Map<String, List<PermissionResponse>> fetchAll() {
        List<Permission> permissions = permissionRepository.findAllByOrderByModuleAscActionAsc();

        return permissions.stream()
                .map(p -> modelMapper.map(p, PermissionResponse.class))
                .collect(Collectors.groupingBy(
                        PermissionResponse::getModule,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }
}
