package com.possystem.businesstype;

import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import com.possystem.module.AdditionalModule;
import com.possystem.module.AdditionalModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessTypeService {

    private final BusinessTypeRepository businessTypeRepository;
    private final BusinessTypeModuleRepository businessTypeModuleRepository;
    private final AdditionalModuleRepository additionalModuleRepository;

    // ==================== CRUD ====================

    @Transactional
    public BusinessTypeResponse save(BusinessTypeRequest request) {
        if (request.getId() != null) {
            return updateBusinessType(request);
        }
        return createBusinessType(request);
    }

    public ListResponse<BusinessTypeResponse> fetch(FetchRequest request) {
        if (request.getId() != null) {
            BusinessType bt = businessTypeRepository.findByIdAndIsActiveTrue(request.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Business type not found"));
            List<BusinessTypeResponse> result = List.of(buildResponse(bt));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<BusinessType> all = businessTypeRepository.searchAll(search);
            List<BusinessTypeResponse> responses = all.stream()
                    .map(this::buildResponse)
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<BusinessType> page = businessTypeRepository.searchAll(search, pageRequest);
        Page<BusinessTypeResponse> responsePage = page.map(this::buildResponse);
        return ListResponse.from(responsePage);
    }

    @Transactional
    public void delete(UUID id) {
        BusinessType bt = businessTypeRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Business type not found"));
        bt.setIsActive(false);
        businessTypeRepository.save(bt);
    }

    public List<BusinessTypeResponse.ModuleInfo> getAvailableModules(UUID businessTypeId) {
        businessTypeRepository.findByIdAndIsActiveTrue(businessTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Business type not found"));

        List<BusinessTypeModule> additionalBtModules = businessTypeModuleRepository
                .findByBusinessTypeIdAndIsDefaultFalse(businessTypeId);
        return resolveModuleInfos(additionalBtModules);
    }

    // ==================== CREATE / UPDATE ====================

    private BusinessTypeResponse createBusinessType(BusinessTypeRequest request) {
        if (businessTypeRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("A business type with this name already exists");
        }

        BusinessType bt = BusinessType.builder()
                .code(generateCode())
                .name(request.getName())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .isActive(true)
                .build();

        BusinessType saved = businessTypeRepository.save(bt);
        syncModules(saved.getId(), request.getModules());
        return buildResponse(saved);
    }

    private BusinessTypeResponse updateBusinessType(BusinessTypeRequest request) {
        BusinessType bt = businessTypeRepository.findByIdAndIsActiveTrue(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Business type not found"));

        if (!bt.getName().equalsIgnoreCase(request.getName())) {
            if (businessTypeRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), bt.getId())) {
                throw new IllegalArgumentException("A business type with this name already exists");
            }
        }

        bt.setName(request.getName());
        bt.setDescription(request.getDescription());
        bt.setIconUrl(request.getIconUrl());

        BusinessType saved = businessTypeRepository.save(bt);
        syncModules(saved.getId(), request.getModules());
        return buildResponse(saved);
    }

    // ==================== HELPERS ====================

    private void syncModules(UUID businessTypeId, List<BusinessTypeRequest.ModuleEntry> modules) {
        businessTypeModuleRepository.deleteByBusinessTypeId(businessTypeId);

        if (modules == null || modules.isEmpty()) return;

        for (BusinessTypeRequest.ModuleEntry entry : modules) {
            additionalModuleRepository.findById(entry.getModuleId())
                    .orElseThrow(() -> new IllegalArgumentException("Module not found: " + entry.getModuleId()));

            BusinessTypeModule btm = BusinessTypeModule.builder()
                    .businessTypeId(businessTypeId)
                    .additionalModuleId(entry.getModuleId())
                    .isDefault(Boolean.TRUE.equals(entry.getIsDefault()))
                    .build();
            businessTypeModuleRepository.save(btm);
        }
    }

    private BusinessTypeResponse buildResponse(BusinessType bt) {
        List<BusinessTypeModule> defaultBtModules = businessTypeModuleRepository
                .findByBusinessTypeIdAndIsDefaultTrue(bt.getId());
        List<BusinessTypeModule> additionalBtModules = businessTypeModuleRepository
                .findByBusinessTypeIdAndIsDefaultFalse(bt.getId());

        return BusinessTypeResponse.builder()
                .id(bt.getId())
                .code(bt.getCode())
                .name(bt.getName())
                .description(bt.getDescription())
                .iconUrl(bt.getIconUrl())
                .isActive(bt.getIsActive())
                .defaultModules(resolveModuleInfos(defaultBtModules))
                .additionalModules(resolveModuleInfos(additionalBtModules))
                .createdAt(bt.getCreatedAt())
                .updatedAt(bt.getUpdatedAt())
                .build();
    }

    private List<BusinessTypeResponse.ModuleInfo> resolveModuleInfos(List<BusinessTypeModule> btModules) {
        return btModules.stream()
                .map(btm -> {
                    AdditionalModule module = additionalModuleRepository.findById(btm.getAdditionalModuleId())
                            .orElse(null);
                    if (module == null) return null;
                    return BusinessTypeResponse.ModuleInfo.builder()
                            .moduleId(module.getId())
                            .moduleCode(module.getModuleCode())
                            .moduleName(module.getModuleName())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private String generateCode() {
        long count = businessTypeRepository.count();
        String code;
        do {
            count++;
            code = String.format("BT-%04d", count);
        } while (businessTypeRepository.existsByCode(code));
        return code;
    }
}
