package com.invest.application.usecases;

import com.invest.application.commands.UpdateRuleCommand;
import com.invest.application.responses.RuleResponse;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.IndicatorValue;
import com.invest.domain.entities.Rule;
import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.ComparisonOperator;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.domain.exceptions.AccessDeniedException;
import com.invest.domain.exceptions.IncompatibleIndicatorException;
import com.invest.domain.exceptions.InvalidRuleFieldException;
import com.invest.domain.exceptions.RuleAlreadyTriggeredException;
import com.invest.domain.exceptions.RuleNotFoundException;
import com.invest.domain.exceptions.UnknownIndicatorException;
import com.invest.domain.ports.out.repositories.AlertRepository;
import com.invest.domain.ports.out.repositories.AssetRepository;
import com.invest.domain.ports.out.repositories.RuleRepository;
import com.invest.domain.services.AssetTypeIndicatorRegistry;
import com.invest.infrastructure.config.InMemoryAssetTypeIndicatorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateRuleUseCaseImplTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AssetRepository assetRepository;

    private AssetTypeIndicatorRegistry indicatorRegistry;
    private UpdateRuleUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        indicatorRegistry = new InMemoryAssetTypeIndicatorRegistry();
        useCase = new UpdateRuleUseCaseImpl(ruleRepository, alertRepository,
                assetRepository, indicatorRegistry);
    }

    @Test
    void shouldUpdateOwnRuleSuccessfully() {
        Long userId = 1L;
        Long ruleId = 10L;
        var existingRule = fiiRule(ruleId, userId, "XPLG11", IndicatorType.PRICE);
        var command = new UpdateRuleCommand("DIVIDEND_YIELD", ComparisonOperator.LESS_THAN,
                BigDecimal.valueOf(8));

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(existingRule));
        when(assetRepository.findByTicker("XPLG11")).thenReturn(Optional.of(fiiAsset("XPLG11")));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleResponse response = useCase.execute(userId, ruleId, command);

        assertEquals("DIVIDEND_YIELD", response.indicatorType());
        assertEquals(ComparisonOperator.LESS_THAN, response.operator());
        assertEquals(BigDecimal.valueOf(8), response.targetValue());
        verify(ruleRepository).save(any(Rule.class));
    }

    @Test
    void shouldThrowRuleNotFoundException_whenRuleDoesNotExist() {
        Long userId = 1L;
        Long ruleId = 10L;
        var command = new UpdateRuleCommand("PRICE", ComparisonOperator.GREATER_THAN, BigDecimal.TEN);

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.empty());

        assertThrows(RuleNotFoundException.class, () -> useCase.execute(userId, ruleId, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowAccessDeniedException_whenRuleBelongsToAnotherUser() {
        Long ownerUserId = 1L;
        Long attackerUserId = 2L;
        Long ruleId = 10L;
        var existingRule = fiiRule(ruleId, ownerUserId, "XPLG11", IndicatorType.PRICE);
        var command = new UpdateRuleCommand("PRICE", ComparisonOperator.GREATER_THAN, BigDecimal.TEN);

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(existingRule));

        assertThrows(AccessDeniedException.class, () -> useCase.execute(attackerUserId, ruleId, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenIndicatorCodeIsNull() {
        var command = new UpdateRuleCommand(null, ComparisonOperator.GREATER_THAN, BigDecimal.TEN);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(1L, 10L, command));
        verify(ruleRepository, never()).findById(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenOperatorIsNull() {
        var command = new UpdateRuleCommand("PRICE", null, BigDecimal.TEN);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(1L, 10L, command));
        verify(ruleRepository, never()).findById(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenTargetValueIsNull() {
        var command = new UpdateRuleCommand("PRICE", ComparisonOperator.GREATER_THAN, null);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(1L, 10L, command));
        verify(ruleRepository, never()).findById(any());
    }

    @Test
    void shouldPreserveTickerAndGroupIdAfterUpdate() {
        Long userId = 1L;
        Long ruleId = 10L;
        var existingRule = fiiRule(ruleId, userId, "HGLG11", IndicatorType.PRICE);
        existingRule.setGroupId(5L);
        var command = new UpdateRuleCommand("PVP", ComparisonOperator.EQUAL, BigDecimal.valueOf(1));

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(existingRule));
        when(assetRepository.findByTicker("HGLG11")).thenReturn(Optional.of(fiiAsset("HGLG11")));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleResponse response = useCase.execute(userId, ruleId, command);

        assertEquals("HGLG11", response.ticker());
        assertEquals(5L, response.groupId());
    }

    @Test
    void shouldThrowRuleAlreadyTriggeredException_whenRuleHasAlerts() {
        Long userId = 1L;
        Long ruleId = 10L;
        var existingRule = fiiRule(ruleId, userId, "XPLG11", IndicatorType.PRICE);
        var command = new UpdateRuleCommand("DIVIDEND_YIELD", ComparisonOperator.LESS_THAN,
                BigDecimal.valueOf(8));

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(existingRule));
        when(alertRepository.existsByRuleId(ruleId)).thenReturn(true);

        assertThrows(RuleAlreadyTriggeredException.class, () -> useCase.execute(userId, ruleId, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowUnknownIndicatorException_whenIndicatorCodeIsUnknown() {
        Long userId = 1L;
        Long ruleId = 10L;
        var existingRule = fiiRule(ruleId, userId, "XPLG11", IndicatorType.PRICE);
        var command = new UpdateRuleCommand("UNKNOWN_CODE", ComparisonOperator.GREATER_THAN, BigDecimal.TEN);

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(existingRule));
        when(assetRepository.findByTicker("XPLG11")).thenReturn(Optional.of(fiiAsset("XPLG11")));

        assertThrows(UnknownIndicatorException.class, () -> useCase.execute(userId, ruleId, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowIncompatibleIndicatorException_whenIndicatorNotSupportedForAssetType() {
        Long userId = 1L;
        Long ruleId = 10L;
        var existingRule = cryptoRule(ruleId, userId, "BTC", IndicatorType.PRICE);
        var command = new UpdateRuleCommand("DIVIDEND_YIELD", ComparisonOperator.GREATER_THAN, BigDecimal.TEN);

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(existingRule));
        when(assetRepository.findByTicker("BTC")).thenReturn(Optional.of(cryptoAsset("BTC")));

        assertThrows(IncompatibleIndicatorException.class, () -> useCase.execute(userId, ruleId, command));
        verify(ruleRepository, never()).save(any());
    }

    private Rule fiiRule(Long ruleId, Long userId, String ticker, IndicatorType indicatorType) {
        return new Rule(ruleId, userId, ticker, null, indicatorType,
                ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(100),
                true, LocalDateTime.now(), LocalDateTime.now());
    }

    private Rule cryptoRule(Long ruleId, Long userId, String ticker, IndicatorType indicatorType) {
        return new Rule(ruleId, userId, ticker, null, indicatorType,
                ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(50000),
                true, LocalDateTime.now(), LocalDateTime.now());
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
}
