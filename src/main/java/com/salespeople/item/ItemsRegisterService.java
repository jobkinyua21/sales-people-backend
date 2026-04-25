package com.salespeople.item;

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
public class ItemsRegisterService {

    private final ItemsRegisterRepository itemsRegisterRepository;

    public ListResponse<ItemsRegisterResponse> fetch(ItemsFetchRequest request) {
        if (request.getItemRegisterId() != null) {
            ItemsRegister item = itemsRegisterRepository.findById(request.getItemRegisterId())
                    .orElseThrow(() -> new IllegalArgumentException("Item not found"));
            return ListResponse.of(List.of(toResponse(item)));
        }

        if (request.getItemCode() != null) {
            ItemsRegister item = itemsRegisterRepository.findByItemCode(request.getItemCode())
                    .orElseThrow(() -> new IllegalArgumentException("Item not found"));
            return ListResponse.of(List.of(toResponse(item)));
        }

        if (request.getLimit() == null) {
            List<ItemsRegister> all = itemsRegisterRepository.searchAll(request.getSearch());
            return ListResponse.of(all.stream().map(this::toResponse).toList());
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), request.getLimit());
        Page<ItemsRegister> page = itemsRegisterRepository.searchAll(request.getSearch(), pageRequest);
        return ListResponse.from(page.map(this::toResponse));
    }

    @Transactional
    public ItemsRegisterResponse toggleDisabled(Long id) {
        ItemsRegister item = itemsRegisterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        item.setDisabled(!Boolean.TRUE.equals(item.getDisabled()));
        return toResponse(itemsRegisterRepository.save(item));
    }

    private ItemsRegisterResponse toResponse(ItemsRegister i) {
        return ItemsRegisterResponse.builder()
                .itemRegisterId(i.getItemRegisterId())
                .itemCode(i.getItemCode())
                .itemName(i.getItemName())
                .itemUnits(i.getItemUnits())
                .itemUnitsValue(i.getItemUnitsValue())
                .itemUnitsAbbreviations(i.getItemUnitsAbbreviations())
                .previousPrice(i.getPreviousPrice())
                .currentPrice(i.getCurrentPrice())
                .accountNumber(i.getAccountNumber())
                .constraint(i.getConstraint())
                .status(i.getStatus())
                .disabled(i.getDisabled())
                .createdAt(i.getCreatedAt())
                .build();
    }
}
