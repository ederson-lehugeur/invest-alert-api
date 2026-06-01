package com.invest.infrastructure.config;

import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.IndicatorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetTypeConfigurationTest {

    private InMemoryAssetTypeIndicatorRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryAssetTypeIndicatorRegistry();
    }

    @Test
    void fiiShouldSupportExactlyPriceDividendYieldAndPvp() {
        Set<String> supported = indicatorCodes(AssetType.FII);

        assertEquals(Set.of("PRICE", "DIVIDEND_YIELD", "PVP"), supported,
                "FII must support exactly PRICE, DIVIDEND_YIELD, PVP");
    }

    @Test
    void cryptocurrencyShouldSupportOnlyPrice() {
        Set<String> supported = indicatorCodes(AssetType.CRYPTOCURRENCY);

        assertEquals(Set.of("PRICE"), supported,
                "CRYPTOCURRENCY must support only PRICE");
    }

    @Test
    void stockShouldSupportPriceDividendYieldPvpPlAndRoe() {
        Set<String> supported = indicatorCodes(AssetType.STOCK);

        assertEquals(Set.of("PRICE", "DIVIDEND_YIELD", "PVP", "PL", "ROE"), supported,
                "STOCK must support exactly PRICE, DIVIDEND_YIELD, PVP, PL, ROE");
    }

    @Test
    void fiiShouldNotSupportPlOrRoe() {
        assertFalse(registry.supports(AssetType.FII, IndicatorType.PL),
                "FII must not support PL");
        assertFalse(registry.supports(AssetType.FII, IndicatorType.ROE),
                "FII must not support ROE");
    }

    @Test
    void cryptocurrencyShouldNotSupportDividendYieldOrPvp() {
        assertFalse(registry.supports(AssetType.CRYPTOCURRENCY, IndicatorType.DIVIDEND_YIELD),
                "CRYPTOCURRENCY must not support DIVIDEND_YIELD");
        assertFalse(registry.supports(AssetType.CRYPTOCURRENCY, IndicatorType.PVP),
                "CRYPTOCURRENCY must not support PVP");
    }

    @Test
    void allAssetTypesShouldSupportPrice() {
        for (AssetType assetType : AssetType.values()) {
            assertTrue(registry.supports(assetType, IndicatorType.PRICE),
                    "All asset types must support PRICE, but " + assetType + " does not");
        }
    }

    private Set<String> indicatorCodes(AssetType assetType) {
        return registry.getSupportedIndicators(assetType).stream()
                .map(IndicatorType::code)
                .collect(Collectors.toSet());
    }
}
