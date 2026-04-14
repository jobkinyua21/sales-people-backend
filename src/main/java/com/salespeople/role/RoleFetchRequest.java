package com.salespeople.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleFetchRequest {

    private UUID id;
    private String search;

    @Builder.Default
    private int start = 0;

    private Integer limit;
}
