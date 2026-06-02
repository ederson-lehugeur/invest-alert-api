package com.invest.application.usecases;

import com.invest.application.commands.CreateRuleCommand;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.Rule;
import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.ComparisonOperator;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.domain.exceptions.IncompatibleIndicatorException;
import com.invest.domain.exceptions.UnknownIndicatorException;
import com.invest.domain.ports.out.repositories.AssetRepository;
import com.invest.domain.ports.out.repositories.RuleRepository;
import com.invest.infrastructure.config.InMemoryAssetTypeIndicatorRegistry;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature: asset-type-indicator-registry, Property 6: Rule creation/update compatibility enforcement
 * Feature: asset-type-indicator-registry, Property 7: Unknown indicator code rejection
 */
class RuleCompatibilityProperties {

    private final InMemoryAssetTypeIndicatorRegistry registry = new InMemoryAssetTypeIndicatorRegistry();

    private static final Set<String> KNOWN_CODES = Arrays.stream(IndicatorType.values())
            .map(IndicatorType::code)
            .collect(Collectors.toSet());

    /**
     * Property 6: For any (AssetType, IndicatorType) pair, rule creation succeeds iff supports(assetType, indicatorType) is true
     * Validates: Requirements 4.1, 4.2, 4.3
     */
    @Property(tries = 100)
    void ruleCreationRespectsCompatibility(@ForAll AssetType assetType, @ForAll IndicatorType indicatorType) {
        RuleRepository ruleRepository = mock(RuleRepository.class);
        AssetRepository assetRepository = mock(AssetRepository.class);

        Asset asset = Asset.builder()
                .id(1L)
                .ticker("TEST01")
                .name("Test Asset")
                .assetType(assetType)
                .indicatorValues(List.of())
                .updatedAt(LocalDateTime.now())
                .build();

        when(assetRepository.findByTicker("TEST01")).thenReturn(Optional.of(asset));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(inv -> {
            Rule r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        CreateRuleUseCaseImpl useCase = new CreateRuleUseCaseImpl(ruleRepository, assetRepository, registry);
        CreateRuleCommand command = new CreateRuleCommand("TEST01", indicatorType.code(),
                ComparisonOperator.GREATER_THAN, BigDecimal.TEN, null);

        if (registry.supports(assetType, indicatorType)) {
            assertDoesNotThrow(() -> useCase.execute(1L, command));
        } else {
            assertThrows(IncompatibleIndicatorException.class, () -> useCase.execute(1L, command));
        }
    }

    /**
     * Property 7: For any string not a valid IndicatorType code, rule creation throws UnknownIndicatorException
     * Validates: Requirements 4.5
     */
    @Property(tries = 100)
    void unknownIndicatorCodeRejected(@ForAll("unknownCodes") String code) {
        RuleRepository ruleRepository = mock(RuleRepository.class);
        AssetRepository assetRepository = mock(AssetRepository.class);

        Asset asset = Asset.builder()
                .id(1L)
                .ticker("TEST01")
                .name("Test Asset")
                .assetType(AssetType.STOCK)
                .indicatorValues(List.of())
                .updatedAt(LocalDateTime.now())
                .build();

        when(assetRepository.findByTicker("TEST01")).thenReturn(Optional.of(asset));

        CreateRuleUseCaseImpl useCase = new CreateRuleUseCaseImpl(ruleRepository, assetRepository, registry);
        CreateRuleCommand command = new CreateRuleCommand("TEST01", code,
                ComparisonOperator.GREATER_THAN, BigDecimal.TEN, null);

        assertThrows(UnknownIndicatorException.class, () -> useCase.execute(1L, command));
    }

    @Provide
    Arbitrary<String> unknownCodes() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(code -> !KNOWN_CODES.contains(code));
    }
}
