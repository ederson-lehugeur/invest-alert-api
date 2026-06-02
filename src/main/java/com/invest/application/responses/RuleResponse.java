package com.invest.application.responses;

import com.invest.domain.entities.enumerator.ComparisonOperator;

import java.math.BigDecimal;

public record RuleResponse(
        Long id,
        String ticker,
        String indicatorType,
        ComparisonOperator operator,
        BigDecimal targetValue,
        Long groupId,
        boolean active,
        boolean triggered
) {}
