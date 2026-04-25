package com.salespeople.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("""
            SELECT c FROM Customer c
            WHERE c.deleted = false
            AND (COALESCE(:search, '') = ''
                 OR LOWER(c.customerOutletName) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(c.customerContactPerson) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(c.customerContact) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY c.customerOutletName ASC
            """)
    List<Customer> searchAll(@Param("search") String search);

    @Query("""
            SELECT c FROM Customer c
            WHERE c.deleted = false
            AND (COALESCE(:search, '') = ''
                 OR LOWER(c.customerOutletName) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(c.customerContactPerson) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(c.customerContact) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY c.customerOutletName ASC
            """)
    Page<Customer> searchAll(@Param("search") String search, Pageable pageable);
}
