package com.salespeople.common;

public enum UserType {
    ADMIN,
    SALES_PERSON;

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
