package com.invest.application.usecases;

import com.invest.application.responses.AssetResponse;
import com.invest.application.responses.IndicatorValueResponse;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.IndicatorValue;
import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.domain.ports.out.repositories.AssetRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Feature: multi-asset-type-support, Property 5: Fidelidade do mapeamento AssetResponse
 */
class AssetResponseMappingProperties {

    private final AssetRepository assetRepository = mock(AssetRepository.class);
    private final GetAssetUseCaseImpl useCase = new GetAssetUseCaseImpl(assetRepository);

    /**
     * Property 5: For any Asset with any AssetType and any list of IndicatorValues,
     * the AssetResponse returned by GetAssetUseCaseImpl must contain:
     * (a) the correct assetType as String, and
     * (b) a collection of indicators with exactly the same (code, value) pairs.
     * Validates: Requirements 8.1, 8.2
     */
    @Property(tries = 100)
    void assetResponseContainsCorrectAssetTypeAndIndicators(
            @ForAll("assetWithIndicators") Tuple.Tuple2<AssetType, List<IndicatorValue>> input) {

        AssetType assetType = input.get1();
        List<IndicatorValue> indicatorValues = input.get2();

        Asset asset = Asset.builder()
                .id(1L)
                .ticker("TEST11")
                .name("Test Asset")
                .assetType(assetType)
                .indicatorValues(indicatorValues)
                .updatedAt(LocalDateTime.now())
                .build();

        reset(assetRepository);
        when(assetRepository.findByTicker("TEST11")).thenReturn(Optional.of(asset));

        AssetResponse response = useCase.execute("TEST11");

        // (a) assetType must match
        assertEquals(assetType.name(), response.assetType(),
                "AssetResponse.assetType must match the domain AssetType name");

        // (b) indicators must have exactly the same (code, value) pairs
        assertEquals(indicatorValues.size(), response.indicators().size(),
                "AssetResponse.indicators must have the same number of entries as the domain list");

        Map<String, BigDecimal> expectedMap = indicatorValues.stream()
                .collect(Collectors.toMap(iv -> iv.indicatorType().code(), IndicatorValue::value));

        for (IndicatorValueResponse ivr : response.indicators()) {
            assertTrue(expectedMap.containsKey(ivr.code()),
                    "Response indicator code '" + ivr.code() + "' not found in original indicator list");
            assertEquals(expectedMap.get(ivr.code()), ivr.value(),
                    "Value mismatch for indicator '" + ivr.code() + "'");
        }
    }

    @Provide
    Arbitrary<Tuple.Tuple2<AssetType, List<IndicatorValue>>> assetWithIndicators() {
        Arbitrary<List<IndicatorValue>> indicatorListArbitrary = Arbitraries.of(IndicatorType.values())
                .list()
                .uniqueElements()
                .ofMinSize(0)
                .ofMaxSize(5)
                .flatMap(selectedTypes ->
                        Arbitraries.just(
                                selectedTypes.stream()
                                        .map(type -> new IndicatorValue(
                                                type,
                                                BigDecimal.valueOf(Math.random() * 1000)
                                                        .setScale(4, RoundingMode.HALF_UP)
                                        ))
                                        .collect(Collectors.toList())
                        )
                );

        return Arbitraries.of(AssetType.values())
                .flatMap(assetType -> indicatorListArbitrary.map(ivs -> Tuple.of(assetType, ivs)));
    }
}
