package com.invest.application.responses;

import com.invest.domain.entities.enumerator.SubscriptionPlan;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        String name,
        String email,
        SubscriptionPlan subscriptionPlan,
        Set<String> roles,
        LocalDateTime createdAt
) {}
