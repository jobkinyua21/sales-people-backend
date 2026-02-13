package com.possystem.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListResponse<T> {

    private boolean success;
    private long count;
    private Map<String, List<T>> data;

    public static <T> ListResponse<T> from(Page<T> page) {
        return ListResponse.<T>builder()
                .success(true)
                .count(page.getTotalElements())
                .data(Map.of("result", page.getContent()))
                .build();
    }

    public static <T> ListResponse<T> of(List<T> items) {
        return ListResponse.<T>builder()
                .success(true)
                .count(items.size())
                .data(Map.of("result", items))
                .build();
    }
}
