package com.possystem.subscription;

import com.possystem.common.ListResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public SubscriptionPlanResponse save(SubscriptionPlanRequest request) {
        SubscriptionPlan plan;

        if (request.getId() != null) {
            // Update
            plan = subscriptionPlanRepository.findById(request.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found"));
            modelMapper.map(request, plan);
        } else {
            // Create
            plan = modelMapper.map(request, SubscriptionPlan.class);
            plan.setPlanCode(generatePlanCode());
            if (plan.getCurrency() == null) plan.setCurrency("KES");
            if (plan.getStatus() == null) plan.setStatus("ACTIVE");
        }

        SubscriptionPlan saved = subscriptionPlanRepository.save(plan);
        return modelMapper.map(saved, SubscriptionPlanResponse.class);
    }

    public SubscriptionPlanResponse getById(UUID id) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found"));
        return modelMapper.map(plan, SubscriptionPlanResponse.class);
    }

    public ListResponse<SubscriptionPlanResponse> fetch(int start, Integer limit, String search) {
        if (limit == null) {
            // Fetch all without pagination
            List<SubscriptionPlan> all = subscriptionPlanRepository.searchAll(search);
            List<SubscriptionPlanResponse> responses = all.stream()
                    .map(plan -> modelMapper.map(plan, SubscriptionPlanResponse.class))
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(start, limit);
        Page<SubscriptionPlan> page = subscriptionPlanRepository.searchAll(search, pageRequest);

        Page<SubscriptionPlanResponse> responsePage = page.map(
                plan -> modelMapper.map(plan, SubscriptionPlanResponse.class));

        return ListResponse.from(responsePage);
    }

    @Transactional
    public void delete(UUID id) {
        if (!subscriptionPlanRepository.existsById(id)) {
            throw new IllegalArgumentException("Subscription plan not found");
        }
        subscriptionPlanRepository.deleteById(id);
    }

    private String generatePlanCode() {
        long count = subscriptionPlanRepository.count();
        String code;
        do {
            count++;
            code = String.format("PLN-%04d", count);
        } while (subscriptionPlanRepository.existsByPlanCode(code));
        return code;
    }
}
