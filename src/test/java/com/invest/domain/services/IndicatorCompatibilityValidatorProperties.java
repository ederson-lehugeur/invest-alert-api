package com.invest.domain.services;

import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.domain.exceptions.IncompatibleIndicatorException;
import com.invest.infrastructure.config.InMemoryAssetTypeIndicatorRegistry;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Feature: multi-asset-type-support, Property 3: Rejeicao de indicador incompativel com AssetType
 * Feature: multi-asset-type-support, Property 4: Aceitacao de indicador compativel com AssetType
 */
class IndicatorCompatibilityValidatorProperties {

    private final InMemoryAssetTypeIndicatorRegistry registry = new InMemoryAssetTypeIndicatorRegistry();

    /**
     * Property 3: For any AssetType and any IndicatorType not in its supported set,
     * validate() must throw IncompatibleIndicatorException.
     * Validates: Requirements 4.1, 4.3
     */
    @Property(tries = 100)
    void incompatibleIndicatorAlwaysThrows(
            @ForAll("incompatiblePairs") Tuple.Tuple2<AssetType, IndicatorType> pair) {

        AssetType assetType = pair.get1();
        IndicatorType indicatorType = pair.get2();

        assertThrows(
                IncompatibleIndicatorException.class,
                () -> registry.validate(assetType, indicatorType),
                "Expected IncompatibleIndicatorException for " + indicatorType.code()
                        + " with asset type " + assetType
        );
    }

    /**
     * Property 4: For any AssetType and any IndicatorType in its supported set,
     * validate() must complete without throwing any exception.
     * Validates: Requirements 4.2
     */
    @Property(tries = 100)
    void compatibleIndicatorNeverThrows(
            @ForAll("compatiblePairs") Tuple.Tuple2<AssetType, IndicatorType> pair) {

        AssetType assetType = pair.get1();
        IndicatorType indicatorType = pair.get2();

        assertDoesNotThrow(
                () -> registry.validate(assetType, indicatorType),
                "Expected no exception for " + indicatorType.code()
                        + " with asset type " + assetType
        );
    }

    @Provide
    Arbitrary<Tuple.Tuple2<AssetType, IndicatorType>> incompatiblePairs() {
        Set<IndicatorType> all = registry.findAll();

        // Only include asset types that have at least one incompatible indicator
        List<AssetType> assetTypesWithIncompatible = Arrays.stream(AssetType.values())
                .filter(assetType -> {
                    Set<IndicatorType> supported = registry.getSupportedIndicators(assetType);
                    return all.stream().anyMatch(i -> !supported.contains(i));
                })
                .toList();

        return Arbitraries.of(assetTypesWithIncompatible).flatMap(assetType -> {
            Set<IndicatorType> supported = registry.getSupportedIndicators(assetType);

            Set<IndicatorType> incompatible = all.stream()
                    .filter(i -> !supported.contains(i))
                    .collect(Collectors.toSet());

            return Arbitraries.of(incompatible.toArray(new IndicatorType[0]))
                    .map(indicatorType -> Tuple.of(assetType, indicatorType));
        });
    }

    @Provide
    Arbitrary<Tuple.Tuple2<AssetType, IndicatorType>> compatiblePairs() {
        return Arbitraries.of(AssetType.values()).flatMap(assetType -> {
            Set<IndicatorType> supported = registry.getSupportedIndicators(assetType);
            return Arbitraries.of(supported.toArray(new IndicatorType[0]))
                    .map(indicatorType -> Tuple.of(assetType, indicatorType));
        });
    }
}
