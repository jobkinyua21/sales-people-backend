package com.salespeople.salesperson;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesPersonRepository extends JpaRepository<SalesPerson, Long> {

    Optional<SalesPerson> findByStaffId(Integer staffId);

    Optional<SalesPerson> findBySalesPersonNumber(Integer salesPersonNumber);
}
