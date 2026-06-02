package com.invest.infrastructure.config;

import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.domain.exceptions.IncompatibleIndicatorException;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Feature: asset-type-indicator-registry, Property 3: validate() consistency
 * Feature: asset-type-indicator-registry, Property 4: findAll() returns all values
 * Feature: asset-type-indicator-registry, Property 5: findByCode() delegates to IndicatorType.fromCode()
 */
class AssetTypeIndicatorRegistryProperties {

    private final InMemoryAssetTypeIndicatorRegistry registry = new InMemoryAssetTypeIndicatorRegistry();

    /**
     * Property 3: For any (AssetType, IndicatorType) pair, validate() throws iff indicatorType NOT in getSupportedIndicators(assetType)
     * Validates: Requirements 2.3, 2.6, 3.8
     */
    @Property(tries = 100)
    void validateConsistentWithGetSupportedIndicators(@ForAll AssetType assetType, @ForAll IndicatorType indicatorType) {
        Set<IndicatorType> supported = registry.getSupportedIndicators(assetType);

        if (supported.contains(indicatorType)) {
            assertDoesNotThrow(() -> registry.validate(assetType, indicatorType));
        } else {
            assertThrows(IncompatibleIndicatorException.class, () -> registry.validate(assetType, indicatorType));
        }
    }

    /**
     * Property 4: findAll() equals EnumSet.allOf(IndicatorType.class)
     * Validates: Requirements 2.4, 3.7
     */
    @Property(tries = 100)
    void findAllReturnsAllIndicatorTypes() {
        assertEquals(EnumSet.allOf(IndicatorType.class), registry.findAll());
    }

    /**
     * Property 5: For any string input, registry.findByCode(input) equals IndicatorType.fromCode(input)
     * Validates: Requirements 2.5, 3.6
     */
    @Property(tries = 100)
    void findByCodeDelegatesToFromCode(@ForAll String code) {
        assertEquals(IndicatorType.fromCode(code), registry.findByCode(code));
    }
}
