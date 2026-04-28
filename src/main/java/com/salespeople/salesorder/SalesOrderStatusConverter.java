package com.salespeople.salesorder;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SalesOrderStatusConverter implements AttributeConverter<SalesOrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(SalesOrderStatus status) {
        if (status == null) return null;
        return switch (status) {
            case NEW -> "New";
            case PENDING -> "Pending";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            case POSTED -> "Posted";
            case CANCELLED -> "Cancelled";
            case ARCHIVED -> "Archived";
            case IN_TRANSIT -> "In Transit";
        };
    }

    @Override
    public SalesOrderStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        return switch (dbValue) {
            case "New" -> SalesOrderStatus.NEW;
            case "Pending" -> SalesOrderStatus.PENDING;
            case "Approved" -> SalesOrderStatus.APPROVED;
            case "Rejected" -> SalesOrderStatus.REJECTED;
            case "Posted" -> SalesOrderStatus.POSTED;
            case "Cancelled" -> SalesOrderStatus.CANCELLED;
            case "Archived" -> SalesOrderStatus.ARCHIVED;
            case "In Transit" -> SalesOrderStatus.IN_TRANSIT;
            default -> throw new IllegalArgumentException("Unknown status: " + dbValue);
        };
    }
}
