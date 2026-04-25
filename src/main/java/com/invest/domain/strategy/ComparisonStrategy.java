package com.invest.domain.strategy;

import java.math.BigDecimal;

public interface ComparisonStrategy {
    boolean evaluate(BigDecimal currentValue, BigDecimal targetValue);
}
