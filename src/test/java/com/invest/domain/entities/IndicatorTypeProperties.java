package com.invest.domain.entities;

import com.invest.domain.entities.enumerator.IndicatorType;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Feature: asset-type-indicator-registry, Property 1: IndicatorType.fromCode round-trip
 * Feature: asset-type-indicator-registry, Property 2: IndicatorType.fromCode rejects invalid codes
 */
class IndicatorTypeProperties {

    /**
     * Property 1: For any IndicatorType value, fromCode(indicatorType.code()) returns Optional.of(indicatorType)
     * Validates: Requirements 1.3, 1.4
     */
    @Property(tries = 100)
    void fromCodeRoundTrip(@ForAll IndicatorType indicatorType) {
        Optional<IndicatorType> result = IndicatorType.fromCode(indicatorType.code());
        assertEquals(Optional.of(indicatorType), result);
    }

    /**
     * Property 2: For any string not equal to any IndicatorType.code() value, fromCode returns Optional.empty()
     * Validates: Requirements 1.5
     */
    @Property(tries = 100)
    void fromCodeRejectsInvalidCodes(@ForAll String code) {
        boolean isValidCode = Arrays.stream(IndicatorType.values())
                .anyMatch(it -> it.code().equals(code));

        if (!isValidCode) {
            assertEquals(Optional.empty(), IndicatorType.fromCode(code));
        }
    }
}
