package com.invest.domain.strategy;

import java.math.BigDecimal;

public class EqualStrategy implements ComparisonStrategy {

    @Override
    public boolean evaluate(BigDecimal currentValue, BigDecimal targetValue) {
        return currentValue.compareTo(targetValue) == 0;
    }
}
