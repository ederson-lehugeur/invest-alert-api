package com.invest.application.responses;

import com.invest.domain.entities.enumerator.RuleField;
import com.invest.domain.entities.enumerator.ComparisonOperator;

import java.math.BigDecimal;

public record RuleResponse(
        Long id,
        String ticker,
        RuleField field,
        ComparisonOperator operator,
        BigDecimal targetValue,
        Long groupId,
        boolean active,
        boolean triggered
) {}
