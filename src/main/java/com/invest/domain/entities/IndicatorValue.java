package com.invest.domain.entities;

import com.invest.domain.entities.enumerator.IndicatorType;

import java.math.BigDecimal;
import java.util.Objects;

public record IndicatorValue(IndicatorType indicatorType, BigDecimal value) {

    public IndicatorValue {
        Objects.requireNonNull(indicatorType, "indicatorType must not be null");
        Objects.requireNonNull(value, "value must not be null");
    }
}
