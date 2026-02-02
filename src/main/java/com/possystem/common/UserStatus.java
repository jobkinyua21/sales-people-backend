package com.possystem.common;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum UserStatus {
    PENDING,
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    DELETED
}
