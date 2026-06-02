package com.invest.integration;

import com.invest.adapters.persistence.repositories.JpaAssetRepository;
import com.invest.adapters.persistence.repositories.JpaRuleRepository;
import com.invest.domain.entities.enumerator.AssetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying that the database schema and seed data are correct.
 * Validates that assets have proper asset_type, indicator values, and rules
 * reference valid indicator_type values.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Schema and seed data verification tests")
class SchemaIntegrationTest {

    @Autowired
    private JpaAssetRepository assetRepository;

    @Autowired
    private JpaRuleRepository ruleRepository;

    @Test
    @DisplayName("FII assets have asset_type = FII")
    void fiiAssetsShouldHaveCorrectAssetType() {
        var assets = assetRepository.findAll();

        assertFalse(assets.isEmpty(), "Expected at least one asset in the database");

        assets.forEach(asset ->
                assertEquals(AssetType.FII, asset.getAssetType(),
                        "Asset " + asset.getTicker() + " should have asset_type = FII")
        );
    }

    @Test
    @DisplayName("asset_indicator_value contains PRICE, DIVIDEND_YIELD, PVP for each FII asset")
    void assetIndicatorValueContainsExpectedValues() {
        var xplg11 = assetRepository.findByTicker("XPLG11");
        assertTrue(xplg11.isPresent(), "XPLG11 asset must exist in the database");

        var indicatorTypes = xplg11.get().getIndicatorValues().stream()
                .map(iv -> iv.getId().getIndicatorType())
                .toList();

        assertTrue(indicatorTypes.contains("PRICE"),
                "XPLG11 must have PRICE indicator value");
        assertTrue(indicatorTypes.contains("DIVIDEND_YIELD"),
                "XPLG11 must have DIVIDEND_YIELD indicator value");
        assertTrue(indicatorTypes.contains("PVP"),
                "XPLG11 must have PVP indicator value");
    }

    @Test
    @DisplayName("All seeded assets have indicator values for PRICE, DIVIDEND_YIELD, and PVP")
    void allFiiAssetsShouldHaveThreeIndicatorValues() {
        var assets = assetRepository.findAll();

        for (var asset : assets) {
            var indicatorTypes = asset.getIndicatorValues().stream()
                    .map(iv -> iv.getId().getIndicatorType())
                    .toList();

            assertTrue(indicatorTypes.contains("PRICE"),
                    asset.getTicker() + " must have PRICE indicator");
            assertTrue(indicatorTypes.contains("DIVIDEND_YIELD"),
                    asset.getTicker() + " must have DIVIDEND_YIELD indicator");
            assertTrue(indicatorTypes.contains("PVP"),
                    asset.getTicker() + " must have PVP indicator");
        }
    }

    @Test
    @DisplayName("Rules have valid indicator_type values")
    void rulesShouldHaveValidIndicatorType() {
        var rules = ruleRepository.findAll();

        assertFalse(rules.isEmpty(), "Expected at least one rule in the database");

        var indicatorTypes = rules.stream()
                .map(rule -> rule.getIndicatorType())
                .toList();

        Set<String> validIndicatorTypes = Set.of("PRICE", "DIVIDEND_YIELD", "PVP");
        indicatorTypes.forEach(type ->
                assertTrue(validIndicatorTypes.contains(type),
                        "Rule indicator_type '" + type + "' must be one of the valid values: " + validIndicatorTypes)
        );
    }

    @Test
    @DisplayName("Rules referencing PRICE indicator have indicator_type = PRICE")
    void ruleWithPriceIndicatorType() {
        var rules = ruleRepository.findAll();

        var priceRules = rules.stream()
                .filter(rule -> "PRICE".equals(rule.getIndicatorType()))
                .toList();

        assertFalse(priceRules.isEmpty(), "Expected at least one rule with indicator_type = PRICE");

        priceRules.forEach(rule -> {
            assertNotNull(rule.getIndicatorType(), "indicator_type must not be null");
            assertEquals("PRICE", rule.getIndicatorType(),
                    "Rule id=" + rule.getId() + " must have indicator_type = PRICE");
        });
    }

    @Test
    @DisplayName("Rules referencing DIVIDEND_YIELD indicator have indicator_type = DIVIDEND_YIELD")
    void ruleWithDividendYieldIndicatorType() {
        var rules = ruleRepository.findAll();

        var dyRules = rules.stream()
                .filter(rule -> "DIVIDEND_YIELD".equals(rule.getIndicatorType()))
                .toList();

        assertFalse(dyRules.isEmpty(), "Expected at least one rule with indicator_type = DIVIDEND_YIELD");

        dyRules.forEach(rule ->
                assertEquals("DIVIDEND_YIELD", rule.getIndicatorType(),
                        "Rule id=" + rule.getId() + " must have indicator_type = DIVIDEND_YIELD")
        );
    }

    @Test
    @DisplayName("Rules referencing PVP indicator have indicator_type = PVP")
    void ruleWithPvpIndicatorType() {
        var rules = ruleRepository.findAll();

        var pvpRules = rules.stream()
                .filter(rule -> "PVP".equals(rule.getIndicatorType()))
                .toList();

        assertFalse(pvpRules.isEmpty(), "Expected at least one rule with indicator_type = PVP");

        pvpRules.forEach(rule ->
                assertEquals("PVP", rule.getIndicatorType(),
                        "Rule id=" + rule.getId() + " must have indicator_type = PVP")
        );
    }
}
