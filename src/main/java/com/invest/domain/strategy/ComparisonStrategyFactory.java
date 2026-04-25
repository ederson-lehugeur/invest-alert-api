package com.invest.domain.strategy;

import com.invest.domain.entities.ComparisonOperator;

public class ComparisonStrategyFactory {

    private ComparisonStrategyFactory() {
    }

    public static ComparisonStrategy create(ComparisonOperator operator) {
        return switch (operator) {
            case GREATER_THAN -> new GreaterThanStrategy();
            case LESS_THAN -> new LessThanStrategy();
            case GREATER_THAN_OR_EQUAL -> new GreaterThanOrEqualStrategy();
            case LESS_THAN_OR_EQUAL -> new LessThanOrEqualStrategy();
            case EQUAL -> new EqualStrategy();
        };
    }
}
