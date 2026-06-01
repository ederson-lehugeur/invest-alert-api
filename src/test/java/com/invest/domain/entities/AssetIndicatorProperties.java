package com.invest.domain.entities;

import com.invest.application.responses.AssetResponse;
import com.invest.application.responses.IndicatorValueResponse;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Feature: multi-asset-type-support, Property 1: getValueByIndicator round-trip
 * Feature: multi-asset-type-support, Property 5: Fidelidade do mapeamento AssetResponse
 */
class AssetIndicatorProperties {

    /**
     * Property 1: For any list of IndicatorValues used to build an Asset,
     * calling getValueByIndicator(indicatorType) for each indicator present must return
     * the corresponding value, and for any absent indicator must return Optional.empty().
     * Validates: Requirements 2.4, 6.4
     */
    @Property(tries = 100)
    void getValueByIndicatorRoundTrip(@ForAll("indicatorValueLists") List<IndicatorValue> indicatorValues) {
        Asset asset = Asset.builder()
                .id(1L)
                .ticker("TEST11")
                .name("Test Asset")
                .assetType(AssetType.FII)
                .indicatorValues(indicatorValues)
                .updatedAt(LocalDateTime.now())
                .build();

        for (IndicatorValue iv : indicatorValues) {
            Optional<BigDecimal> result = asset.getValueByIndicator(iv.indicatorType());
            assertTrue(result.isPresent(),
                    "Expected value for indicator " + iv.indicatorType().code() + " to be present");
            assertEquals(iv.value(), result.get(),
                    "Expected value for indicator " + iv.indicatorType().code() + " to match");
        }
    }

    /**
     * Property 1b: Absent indicators must return Optional.empty().
     * Validates: Requirements 2.4, 6.4
     */
    @Property(tries = 100)
    void absentIndicatorReturnsEmpty(@ForAll("indicatorValueLists") List<IndicatorValue> indicatorValues) {
        Asset asset = Asset.builder()
                .id(1L)
                .ticker("TEST11")
                .name("Test Asset")
                .assetType(AssetType.FII)
                .indicatorValues(indicatorValues)
                .updatedAt(LocalDateTime.now())
                .build();

        // Find an IndicatorType not in the list
        List<IndicatorType> presentTypes = indicatorValues.stream()
                .map(IndicatorValue::indicatorType)
                .toList();

        for (IndicatorType type : IndicatorType.values()) {
            if (!presentTypes.contains(type)) {
                Optional<BigDecimal> result = asset.getValueByIndicator(type);
                assertTrue(result.isEmpty(),
                        "Expected Optional.empty() for absent indicator " + type.code());
                break;
            }
        }
    }

    /**
     * Property 5: For any Asset with any AssetType and any list of IndicatorValues,
     * the mapped AssetResponse must contain the correct assetType as String and
     * a collection of indicators with exactly the same (code, value) pairs.
     * Validates: Requirements 8.1, 8.2
     */
    @Property(tries = 100)
    void assetResponseMappingFidelity(
            @ForAll("assetTypes") AssetType assetType,
            @ForAll("indicatorValueLists") List<IndicatorValue> indicatorValues) {

        Asset asset = Asset.builder()
                .id(1L)
                .ticker("TEST11")
                .name("Test Asset")
                .assetType(assetType)
                .indicatorValues(indicatorValues)
                .updatedAt(LocalDateTime.now())
                .build();

        List<IndicatorValueResponse> indicators = asset.getIndicatorValues().stream()
                .map(iv -> new IndicatorValueResponse(iv.indicatorType().code(), iv.value()))
                .toList();

        AssetResponse response = new AssetResponse(
                asset.getTicker(),
                asset.getName(),
                asset.getAssetType().name(),
                indicators,
                asset.getUpdatedAt()
        );

        assertEquals(assetType.name(), response.assetType(),
                "AssetResponse.assetType must match the domain AssetType name");

        assertEquals(indicatorValues.size(), response.indicators().size(),
                "AssetResponse.indicators must have the same number of entries");

        Map<String, BigDecimal> expectedMap = indicatorValues.stream()
                .collect(Collectors.toMap(iv -> iv.indicatorType().code(), IndicatorValue::value));

        for (IndicatorValueResponse ivr : response.indicators()) {
            assertTrue(expectedMap.containsKey(ivr.code()),
                    "Response indicator code " + ivr.code() + " not found in original list");
            assertEquals(expectedMap.get(ivr.code()), ivr.value(),
                    "Value mismatch for indicator " + ivr.code());
        }
    }

    @Provide
    Arbitrary<List<IndicatorValue>> indicatorValueLists() {
        return Arbitraries.of(IndicatorType.values())
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
                                                        .setScale(4, java.math.RoundingMode.HALF_UP)
                                        ))
                                        .collect(Collectors.toList())
                        )
                );
    }

    @Provide
    Arbitrary<AssetType> assetTypes() {
        return Arbitraries.of(AssetType.values());
    }
}
