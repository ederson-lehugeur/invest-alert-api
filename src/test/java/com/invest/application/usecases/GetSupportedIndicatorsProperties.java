package com.invest.application.usecases;

import com.invest.domain.entities.enumerator.AssetType;
import com.invest.infrastructure.config.InMemoryAssetTypeIndicatorRegistry;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Feature: asset-type-indicator-registry, Property 9: Supported indicators response ordering
 */
class GetSupportedIndicatorsProperties {

    private final InMemoryAssetTypeIndicatorRegistry registry = new InMemoryAssetTypeIndicatorRegistry();
    private final GetSupportedIndicatorsUseCaseImpl useCase = new GetSupportedIndicatorsUseCaseImpl(registry);

    /**
     * Property 9: For any AssetType, the list returned by GetSupportedIndicatorsUseCaseImpl is sorted alphabetically
     * Validates: Requirements 8.4
     */
    @Property(tries = 100)
    void supportedIndicatorsAreSortedAlphabetically(@ForAll AssetType assetType) {
        List<String> result = useCase.execute(assetType.name());

        List<String> sorted = new ArrayList<>(result);
        Collections.sort(sorted);

        assertEquals(sorted, result);
    }
}
