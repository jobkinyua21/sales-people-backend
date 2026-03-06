package com.possystem.supplier;

import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import com.possystem.security.SecurityContextUtil;
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
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ModelMapper modelMapper;

    // ==================== CRUD ====================

    @Transactional
    public SupplierResponse save(SupplierRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            return updateSupplier(request, shopId);
        }
        return createSupplier(request, shopId);
    }

    public ListResponse<SupplierResponse> fetch(FetchRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            Supplier supplier = supplierRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
            List<SupplierResponse> result = List.of(modelMapper.map(supplier, SupplierResponse.class));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<Supplier> all = supplierRepository.searchAll(shopId, search);
            List<SupplierResponse> responses = all.stream()
                    .map(s -> modelMapper.map(s, SupplierResponse.class))
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<Supplier> page = supplierRepository.searchAll(shopId, search, pageRequest);
        Page<SupplierResponse> responsePage = page.map(s -> modelMapper.map(s, SupplierResponse.class));
        return ListResponse.from(responsePage);
    }

    @Transactional
    public void delete(UUID id) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        Supplier supplier = supplierRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
        supplier.setIsActive(false);
        supplier.setStatus(SupplierStatus.INACTIVE);
        supplierRepository.save(supplier);
    }

    @Transactional
    public int bulkDelete(List<UUID> ids) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        List<Supplier> suppliers = supplierRepository.findAllByIdInAndShopIdAndIsActiveTrue(ids, shopId);
        if (suppliers.isEmpty()) {
            throw new IllegalArgumentException("No suppliers found for the given IDs");
        }
        for (Supplier supplier : suppliers) {
            supplier.setIsActive(false);
            supplier.setStatus(SupplierStatus.INACTIVE);
        }
        supplierRepository.saveAll(suppliers);
        return suppliers.size();
    }

    // ==================== CREATE / UPDATE ====================

    private SupplierResponse createSupplier(SupplierRequest request, UUID shopId) {
        if (supplierRepository.existsByShopIdAndSupplierNameIgnoreCaseAndIsActiveTrue(shopId, request.getSupplierName())) {
            throw new IllegalArgumentException("A supplier with this name already exists");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (supplierRepository.existsByShopIdAndEmailIgnoreCaseAndIsActiveTrue(shopId, request.getEmail())) {
                throw new IllegalArgumentException("A supplier with this email already exists");
            }
        }

        Supplier supplier = modelMapper.map(request, Supplier.class);
        supplier.setShopId(shopId);
        supplier.setSupplierCode(generateSupplierCode(shopId));
        if (supplier.getStatus() == null) supplier.setStatus(SupplierStatus.ACTIVE);

        Supplier saved = supplierRepository.save(supplier);
        return modelMapper.map(saved, SupplierResponse.class);
    }

    private SupplierResponse updateSupplier(SupplierRequest request, UUID shopId) {
        Supplier supplier = supplierRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        if (request.getSupplierName() != null && !supplier.getSupplierName().equalsIgnoreCase(request.getSupplierName())) {
            if (supplierRepository.existsByShopIdAndSupplierNameIgnoreCaseAndIsActiveTrueAndIdNot(
                    shopId, request.getSupplierName(), supplier.getId())) {
                throw new IllegalArgumentException("A supplier with this name already exists");
            }
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (supplier.getEmail() == null || !supplier.getEmail().equalsIgnoreCase(request.getEmail())) {
                if (supplierRepository.existsByShopIdAndEmailIgnoreCaseAndIsActiveTrueAndIdNot(
                        shopId, request.getEmail(), supplier.getId())) {
                    throw new IllegalArgumentException("A supplier with this email already exists");
                }
            }
        }

        modelMapper.map(request, supplier);

        Supplier saved = supplierRepository.save(supplier);
        return modelMapper.map(saved, SupplierResponse.class);
    }

    // ==================== HELPERS ====================

    private String generateSupplierCode(UUID shopId) {
        long count = supplierRepository.countByShopId(shopId);
        String code;
        do {
            count++;
            code = String.format("SUP-%04d", count);
        } while (supplierRepository.existsByShopIdAndSupplierCode(shopId, code));
        return code;
    }

}
