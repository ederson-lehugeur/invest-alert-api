package com.invest.application.commands;

import com.invest.domain.entities.RuleField;
import com.invest.domain.entities.ComparisonOperator;

import java.math.BigDecimal;

public record CreateRuleCommand(
        String ticker,
        RuleField field,
        ComparisonOperator operator,
        BigDecimal targetValue,
        Long groupId
) {}
