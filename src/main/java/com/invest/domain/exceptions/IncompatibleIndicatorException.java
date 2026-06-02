package com.invest.domain.exceptions;

import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.IndicatorType;

import java.util.Set;
import java.util.stream.Collectors;

public class IncompatibleIndicatorException extends RuntimeException {

    public IncompatibleIndicatorException(AssetType assetType, IndicatorType indicatorType,
                                          Set<IndicatorType> supportedIndicators) {
        super("Indicator '%s' is not supported for asset type '%s'. Supported indicators: %s"
                .formatted(
                        indicatorType.code(),
                        assetType.name(),
                        supportedIndicators.stream()
                                .map(IndicatorType::code)
                                .sorted()
                                .collect(Collectors.joining(", "))
                ));
    }
}
