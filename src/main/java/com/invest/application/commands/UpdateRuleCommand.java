package com.invest.application.commands;

import com.invest.domain.entities.enumerator.RuleField;
import com.invest.domain.entities.enumerator.ComparisonOperator;

import java.math.BigDecimal;

public record UpdateRuleCommand(
        RuleField field,
        ComparisonOperator operator,
        BigDecimal targetValue
) {}
