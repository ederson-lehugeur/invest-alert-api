package com.invest.application.commands;

import com.invest.domain.entities.enumerator.ComparisonOperator;

import java.math.BigDecimal;

public record CreateRuleCommand(
        String ticker,
        String indicatorCode,
        ComparisonOperator operator,
        BigDecimal targetValue,
        Long groupId
) {}
