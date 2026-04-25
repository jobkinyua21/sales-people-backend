package com.salespeople.salesorder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesOrderLineRepository extends JpaRepository<SalesOrderLine, Long> {

    List<SalesOrderLine> findBySalesOrderNumber(Long salesOrderNumber);

    void deleteBySalesOrderNumber(Long salesOrderNumber);
}
