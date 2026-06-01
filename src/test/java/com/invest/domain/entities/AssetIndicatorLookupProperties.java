package com.invest.domain.entities;

import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.IndicatorType;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Feature: asset-type-indicator-registry, Property 8: Asset.getValueByIndicator lookup correctness
 */
class AssetIndicatorLookupProperties {

    /**
     * Property 8: For any Asset with IndicatorValue entries, getValueByIndicator(type) returns the value if present, Optional.empty() otherwise
     * Validates: Requirements 5.3, 5.4, 5.5
     */
    @Property(tries = 100)
    void getValueByIndicatorReturnsCorrectResult(
            @ForAll("indicatorSubsets") Set<IndicatorType> presentIndicators,
            @ForAll IndicatorType queryIndicator) {

        List<IndicatorValue> values = presentIndicators.stream()
                .map(it -> new IndicatorValue(it, BigDecimal.valueOf(it.ordinal() + 1)))
                .toList();

        Asset asset = Asset.builder()
                .id(1L)
                .ticker("TEST01")
                .name("Test")
                .assetType(AssetType.STOCK)
                .indicatorValues(values)
                .updatedAt(LocalDateTime.now())
                .build();

        Optional<BigDecimal> result = asset.getValueByIndicator(queryIndicator);

        if (presentIndicators.contains(queryIndicator)) {
            assertEquals(Optional.of(BigDecimal.valueOf(queryIndicator.ordinal() + 1)), result);
        } else {
            assertEquals(Optional.empty(), result);
        }
    }

    @Provide
    Arbitrary<Set<IndicatorType>> indicatorSubsets() {
        return Arbitraries.of(IndicatorType.values())
                .set()
                .ofMinSize(0)
                .ofMaxSize(IndicatorType.values().length);
    }
}
