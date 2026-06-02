package com.invest.application.usecases;

import com.invest.application.commands.CreateRuleCommand;
import com.invest.application.responses.RuleResponse;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.IndicatorValue;
import com.invest.domain.entities.Rule;
import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.ComparisonOperator;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.domain.exceptions.AssetNotFoundException;
import com.invest.domain.exceptions.IncompatibleIndicatorException;
import com.invest.domain.exceptions.InvalidRuleFieldException;
import com.invest.domain.exceptions.UnknownIndicatorException;
import com.invest.domain.ports.out.repositories.AssetRepository;
import com.invest.domain.ports.out.repositories.RuleRepository;
import com.invest.domain.services.AssetTypeIndicatorRegistry;
import com.invest.infrastructure.config.InMemoryAssetTypeIndicatorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateRuleUseCaseImplTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private AssetRepository assetRepository;

    private AssetTypeIndicatorRegistry indicatorRegistry;
    private CreateRuleUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        indicatorRegistry = new InMemoryAssetTypeIndicatorRegistry();
        useCase = new CreateRuleUseCaseImpl(ruleRepository, assetRepository, indicatorRegistry);
    }

    @Test
    void shouldCreateRuleWithValidData() {
        var command = new CreateRuleCommand("XPLG11", "PRICE", ComparisonOperator.LESS_THAN,
                BigDecimal.valueOf(100), null);
        var asset = fiiAsset("XPLG11");

        when(assetRepository.findByTicker("XPLG11")).thenReturn(Optional.of(asset));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> {
            Rule r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        RuleResponse response = useCase.execute(10L, command);

        assertEquals(1L, response.id());
        assertEquals("XPLG11", response.ticker());
        assertEquals("PRICE", response.indicatorType());
        assertEquals(ComparisonOperator.LESS_THAN, response.operator());
        assertEquals(BigDecimal.valueOf(100), response.targetValue());
        assertTrue(response.active());
    }

    @Test
    void shouldSaveRuleWithCorrectUserId() {
        var command = new CreateRuleCommand("HGLG11", "DIVIDEND_YIELD", ComparisonOperator.GREATER_THAN,
                BigDecimal.valueOf(9), null);
        var asset = fiiAsset("HGLG11");

        when(assetRepository.findByTicker("HGLG11")).thenReturn(Optional.of(asset));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(42L, command);

        ArgumentCaptor<Rule> captor = ArgumentCaptor.forClass(Rule.class);
        verify(ruleRepository).save(captor.capture());
        assertEquals(42L, captor.getValue().getUserId());
    }

    @Test
    void shouldThrowAssetNotFoundException_whenTickerDoesNotExist() {
        var command = new CreateRuleCommand("INVALID", "PRICE", ComparisonOperator.EQUAL,
                BigDecimal.ONE, null);

        when(assetRepository.findByTicker("INVALID")).thenReturn(Optional.empty());

        assertThrows(AssetNotFoundException.class, () -> useCase.execute(1L, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenIndicatorCodeIsNull() {
        var command = new CreateRuleCommand("XPLG11", null, ComparisonOperator.GREATER_THAN,
                BigDecimal.TEN, null);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(1L, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenIndicatorCodeIsBlank() {
        var command = new CreateRuleCommand("XPLG11", "  ", ComparisonOperator.GREATER_THAN,
                BigDecimal.TEN, null);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(1L, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenOperatorIsNull() {
        var command = new CreateRuleCommand("XPLG11", "PRICE", null,
                BigDecimal.TEN, null);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(1L, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenTargetValueIsNull() {
        var command = new CreateRuleCommand("XPLG11", "PRICE", ComparisonOperator.GREATER_THAN,
                null, null);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(1L, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenTickerIsBlank() {
        var command = new CreateRuleCommand("  ", "PRICE", ComparisonOperator.GREATER_THAN,
                BigDecimal.TEN, null);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(1L, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowUnknownIndicatorException_whenIndicatorCodeIsUnknown() {
        var command = new CreateRuleCommand("XPLG11", "UNKNOWN_CODE", ComparisonOperator.GREATER_THAN,
                BigDecimal.TEN, null);
        var asset = fiiAsset("XPLG11");

        when(assetRepository.findByTicker("XPLG11")).thenReturn(Optional.of(asset));

        assertThrows(UnknownIndicatorException.class, () -> useCase.execute(1L, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowIncompatibleIndicatorException_whenIndicatorNotSupportedForAssetType() {
        var command = new CreateRuleCommand("BTC", "DIVIDEND_YIELD", ComparisonOperator.GREATER_THAN,
                BigDecimal.TEN, null);
        var asset = cryptoAsset("BTC");

        when(assetRepository.findByTicker("BTC")).thenReturn(Optional.of(asset));

        assertThrows(IncompatibleIndicatorException.class, () -> useCase.execute(1L, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldCreateRuleWithGroupId() {
        var command = new CreateRuleCommand("XPLG11", "PVP", ComparisonOperator.LESS_THAN_OR_EQUAL,
                BigDecimal.valueOf(1.2), 5L);
        var asset = fiiAsset("XPLG11");

        when(assetRepository.findByTicker("XPLG11")).thenReturn(Optional.of(asset));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> {
            Rule r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        RuleResponse response = useCase.execute(1L, command);

        assertEquals(5L, response.groupId());
    }

    @Test
    void shouldCreateRuleForStockWithPLIndicator() {
        var command = new CreateRuleCommand("PETR4", "PL", ComparisonOperator.LESS_THAN,
                BigDecimal.valueOf(10), null);
        var asset = stockAsset("PETR4");

        when(assetRepository.findByTicker("PETR4")).thenReturn(Optional.of(asset));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> {
            Rule r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        RuleResponse response = useCase.execute(1L, command);

        assertEquals("PL", response.indicatorType());
    }

    private Asset fiiAsset(String ticker) {
        return Asset.builder()
                .id(1L)
                .ticker(ticker)
                .name("FII " + ticker)
                .assetType(AssetType.FII)
                .indicatorValues(List.of(
                        new IndicatorValue(IndicatorType.PRICE, BigDecimal.valueOf(110)),
                        new IndicatorValue(IndicatorType.DIVIDEND_YIELD, BigDecimal.valueOf(8.5)),
                        new IndicatorValue(IndicatorType.PVP, BigDecimal.valueOf(0.95))
                ))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Asset cryptoAsset(String ticker) {
        return Asset.builder()
                .id(2L)
                .ticker(ticker)
                .name("Crypto " + ticker)
                .assetType(AssetType.CRYPTOCURRENCY)
                .indicatorValues(List.of(
                        new IndicatorValue(IndicatorType.PRICE, BigDecimal.valueOf(50000))
                ))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Asset stockAsset(String ticker) {
        return Asset.builder()
                .id(3L)
                .ticker(ticker)
                .name("Stock " + ticker)
                .assetType(AssetType.STOCK)
                .indicatorValues(List.of(
                        new IndicatorValue(IndicatorType.PRICE, BigDecimal.valueOf(30)),
                        new IndicatorValue(IndicatorType.PL, BigDecimal.valueOf(8)),
                        new IndicatorValue(IndicatorType.ROE, BigDecimal.valueOf(15))
                ))
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
