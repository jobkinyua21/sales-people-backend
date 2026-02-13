package com.possystem.communications;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;
import java.util.UUID;

@Builder
@Getter
@ToString
public class AlertDTO {
    private String phone;
    private String firstName;
    private String email;
    private String templateName;
    private UUID usrId;
    private String message;
    private Map<String, String> placeholders;
}
