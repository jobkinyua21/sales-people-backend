package com.salespeople.customer;

import com.salespeople.common.ListResponse;
import com.salespeople.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse save(CustomerRequest request) {
        if (request.getCustomerId() != null) {
            return update(request);
        }
        return create(request);
    }

    private CustomerResponse create(CustomerRequest request) {
        String currentUser = SecurityContextUtil.getCurrentPrincipal().getUsername();

        Customer customer = Customer.builder()
                .customerOutletName(request.getCustomerOutletName())
                .customerContactPerson(request.getCustomerContactPerson())
                .customerContact(request.getCustomerContact())
                .customerEmail(request.getCustomerEmail() != null ? request.getCustomerEmail() : "null@null")
                .customerLocation(request.getCustomerLocation() != null ? request.getCustomerLocation() : "null")
                .buyOnCredit(request.getBuyOnCredit() != null ? request.getBuyOnCredit() : false)
                .createdBy(currentUser)
                .deleted(false)
                .build();

        return toResponse(customerRepository.save(customer));
    }

    private CustomerResponse update(CustomerRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (Boolean.TRUE.equals(customer.getDeleted())) {
            throw new IllegalArgumentException("Customer not found");
        }

        customer.setCustomerOutletName(request.getCustomerOutletName());
        customer.setCustomerContactPerson(request.getCustomerContactPerson());
        customer.setCustomerContact(request.getCustomerContact());
        if (request.getCustomerEmail() != null) customer.setCustomerEmail(request.getCustomerEmail());
        if (request.getCustomerLocation() != null) customer.setCustomerLocation(request.getCustomerLocation());
        if (request.getBuyOnCredit() != null) customer.setBuyOnCredit(request.getBuyOnCredit());

        return toResponse(customerRepository.save(customer));
    }

    public ListResponse<CustomerResponse> fetch(CustomerFetchRequest request) {
        if (request.getCustomerId() != null) {
            Customer c = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            return ListResponse.of(List.of(toResponse(c)));
        }

        if (request.getLimit() == null) {
            List<Customer> all = customerRepository.searchAll(request.getSearch());
            return ListResponse.of(all.stream().map(this::toResponse).toList());
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), request.getLimit());
        Page<Customer> page = customerRepository.searchAll(request.getSearch(), pageRequest);
        return ListResponse.from(page.map(this::toResponse));
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        customer.setDeleted(true);
        customerRepository.save(customer);
    }

    private CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
                .customerId(c.getCustomerId())
                .customerOutletName(c.getCustomerOutletName())
                .customerContactPerson(c.getCustomerContactPerson())
                .customerContact(c.getCustomerContact())
                .customerEmail(c.getCustomerEmail())
                .customerLocation(c.getCustomerLocation())
                .fullName(c.getFullName())
                .buyOnCredit(c.getBuyOnCredit())
                .balance(c.getBalance())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
