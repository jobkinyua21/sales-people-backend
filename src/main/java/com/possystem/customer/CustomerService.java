package com.possystem.customer;

import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import com.possystem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ModelMapper modelMapper;

    // ==================== CRUD ====================

    @Transactional
    public CustomerResponse save(CustomerRequest request) {
        UUID shopId = getCurrentShopId();

        if (request.getId() != null) {
            return updateCustomer(request, shopId);
        }
        return createCustomer(request, shopId);
    }

    public ListResponse<CustomerResponse> fetch(FetchRequest request) {
        UUID shopId = getCurrentShopId();

        if (request.getId() != null) {
            Customer customer = customerRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            List<CustomerResponse> result = List.of(modelMapper.map(customer, CustomerResponse.class));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<Customer> all = customerRepository.searchAll(shopId, search);
            List<CustomerResponse> responses = all.stream()
                    .map(c -> modelMapper.map(c, CustomerResponse.class))
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<Customer> page = customerRepository.searchAll(shopId, search, pageRequest);
        Page<CustomerResponse> responsePage = page.map(c -> modelMapper.map(c, CustomerResponse.class));
        return ListResponse.from(responsePage);
    }

    @Transactional
    public void delete(UUID id) {
        UUID shopId = getCurrentShopId();
        Customer customer = customerRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        customer.setIsActive(false);
        customer.setStatus(CustomerStatus.INACTIVE);
        customerRepository.save(customer);
    }

    @Transactional
    public int bulkDelete(List<UUID> ids) {
        UUID shopId = getCurrentShopId();
        List<Customer> customers = customerRepository.findAllByIdInAndShopIdAndIsActiveTrue(ids, shopId);
        if (customers.isEmpty()) {
            throw new IllegalArgumentException("No customers found for the given IDs");
        }
        for (Customer customer : customers) {
            customer.setIsActive(false);
            customer.setStatus(CustomerStatus.INACTIVE);
        }
        customerRepository.saveAll(customers);
        return customers.size();
    }

    // ==================== CREATE / UPDATE ====================

    private CustomerResponse createCustomer(CustomerRequest request, UUID shopId) {
        if (customerRepository.existsByShopIdAndCustomerNameIgnoreCaseAndIsActiveTrue(shopId, request.getCustomerName())) {
            throw new IllegalArgumentException("A customer with this name already exists");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (customerRepository.existsByShopIdAndEmailIgnoreCaseAndIsActiveTrue(shopId, request.getEmail())) {
                throw new IllegalArgumentException("A customer with this email already exists");
            }
        }

        Customer customer = modelMapper.map(request, Customer.class);
        customer.setShopId(shopId);
        customer.setCustomerCode(generateCustomerCode(shopId));
        if (customer.getStatus() == null) customer.setStatus(CustomerStatus.ACTIVE);

        Customer saved = customerRepository.save(customer);
        return modelMapper.map(saved, CustomerResponse.class);
    }

    private CustomerResponse updateCustomer(CustomerRequest request, UUID shopId) {
        Customer customer = customerRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (request.getCustomerName() != null && !customer.getCustomerName().equalsIgnoreCase(request.getCustomerName())) {
            if (customerRepository.existsByShopIdAndCustomerNameIgnoreCaseAndIsActiveTrueAndIdNot(
                    shopId, request.getCustomerName(), customer.getId())) {
                throw new IllegalArgumentException("A customer with this name already exists");
            }
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (customer.getEmail() == null || !customer.getEmail().equalsIgnoreCase(request.getEmail())) {
                if (customerRepository.existsByShopIdAndEmailIgnoreCaseAndIsActiveTrueAndIdNot(
                        shopId, request.getEmail(), customer.getId())) {
                    throw new IllegalArgumentException("A customer with this email already exists");
                }
            }
        }

        modelMapper.map(request, customer);

        Customer saved = customerRepository.save(customer);
        return modelMapper.map(saved, CustomerResponse.class);
    }

    // ==================== HELPERS ====================

    private String generateCustomerCode(UUID shopId) {
        long count = customerRepository.countByShopId(shopId);
        String code;
        do {
            count++;
            code = String.format("CUS-%04d", count);
        } while (customerRepository.existsByShopIdAndCustomerCode(shopId, code));
        return code;
    }

    private UUID getCurrentShopId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        UUID shopId = principal.getShopId();
        if (shopId == null) {
            throw new IllegalArgumentException("Shop context is required");
        }
        return shopId;
    }
}
