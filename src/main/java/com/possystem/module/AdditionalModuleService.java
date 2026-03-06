package com.possystem.module;

import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import com.possystem.module.enums.ModuleStatus;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdditionalModuleService {

    private final AdditionalModuleRepository additionalModuleRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public AdditionalModuleResponse save(AdditionalModuleRequest request) {
        AdditionalModule module;

        if (request.getId() != null) {
            module = additionalModuleRepository.findById(request.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Additional module not found"));
            modelMapper.map(request, module);
        } else {
            module = modelMapper.map(request, AdditionalModule.class);
            String code = generateModuleCode(request.getModuleName());
            if (additionalModuleRepository.existsByModuleCode(code)) {
                throw new IllegalArgumentException("Module code '" + code + "' already exists");
            }
            module.setModuleCode(code);
            if (module.getCurrency() == null) module.setCurrency("KES");
            if (module.getStatus() == null) module.setStatus(ModuleStatus.ACTIVE);
        }

        AdditionalModule saved = additionalModuleRepository.save(module);
        return modelMapper.map(saved, AdditionalModuleResponse.class);
    }

    public ListResponse<AdditionalModuleResponse> fetch(FetchRequest request) {
        if (request.getId() != null) {
            AdditionalModule module = additionalModuleRepository.findById(request.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Additional module not found"));
            List<AdditionalModuleResponse> result = List.of(modelMapper.map(module, AdditionalModuleResponse.class));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<AdditionalModule> all = additionalModuleRepository.searchAll(search);
            List<AdditionalModuleResponse> responses = all.stream()
                    .map(m -> modelMapper.map(m, AdditionalModuleResponse.class))
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<AdditionalModule> page = additionalModuleRepository.searchAll(search, pageRequest);
        Page<AdditionalModuleResponse> responsePage = page.map(m -> modelMapper.map(m, AdditionalModuleResponse.class));
        return ListResponse.from(responsePage);
    }

    @Transactional
    public void delete(UUID id) {
        if (!additionalModuleRepository.existsById(id)) {
            throw new IllegalArgumentException("Additional module not found");
        }
        additionalModuleRepository.deleteById(id);
    }

    private String generateModuleCode(String moduleName) {
        return moduleName.trim()
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }
}
