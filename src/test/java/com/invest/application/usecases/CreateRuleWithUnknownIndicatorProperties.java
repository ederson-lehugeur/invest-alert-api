package com.invest.application.usecases;

import com.invest.application.commands.CreateRuleCommand;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.ComparisonOperator;
import com.invest.domain.exceptions.UnknownIndicatorException;
import com.invest.domain.ports.out.repositories.AssetRepository;
import com.invest.domain.ports.out.repositories.RuleRepository;
import com.invest.domain.services.AssetTypeIndicatorRegistry;
import com.invest.infrastructure.config.InMemoryAssetTypeIndicatorRegistry;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature: multi-asset-type-support, Property 2: Rejeicao de indicador desconhecido na criacao de Rule
 */
class CreateRuleWithUnknownIndicatorProperties {

    private static final Set<String> KNOWN_CODES = Set.of("PRICE", "DIVIDEND_YIELD", "PVP", "PL", "ROE");

    private final RuleRepository ruleRepository = mock(RuleRepository.class);
    private final AssetRepository assetRepository = mock(AssetRepository.class);
    private final AssetTypeIndicatorRegistry indicatorRegistry = new InMemoryAssetTypeIndicatorRegistry();

    private final CreateRuleUseCaseImpl useCase =
            new CreateRuleUseCaseImpl(ruleRepository, assetRepository, indicatorRegistry);

    /**
     * Property 2: For any indicator code not registered in AssetTypeIndicatorRegistry,
     * attempting to create a Rule must throw UnknownIndicatorException,
     * and RuleRepository.save() must never be called.
     * Validates: Requirements 2.5
     */
    @Property(tries = 100)
    void unknownIndicatorCodeAlwaysThrows(@ForAll("unknownIndicatorCodes") String unknownCode) {
        Asset asset = Asset.builder()
                .id(1L)
                .ticker("XPLG11")
                .name("XP Log FII")
                .assetType(AssetType.FII)
                .indicatorValues(List.of())
                .updatedAt(LocalDateTime.now())
                .build();

        reset(ruleRepository, assetRepository);
        when(assetRepository.findByTicker("XPLG11")).thenReturn(Optional.of(asset));

        CreateRuleCommand command = new CreateRuleCommand(
                "XPLG11",
                unknownCode,
                ComparisonOperator.GREATER_THAN,
                BigDecimal.valueOf(100),
                null
        );

        assertThrows(
                UnknownIndicatorException.class,
                () -> useCase.execute(1L, command),
                "Expected UnknownIndicatorException for unknown indicator code: " + unknownCode
        );

        verify(ruleRepository, never()).save(any());
    }

    @Provide
    Arbitrary<String> unknownIndicatorCodes() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(code -> !KNOWN_CODES.contains(code));
    }
}
