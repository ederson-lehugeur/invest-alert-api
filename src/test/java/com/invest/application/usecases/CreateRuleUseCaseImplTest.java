package com.invest.application.usecases;

import com.invest.application.commands.CreateRuleCommand;
import com.invest.application.responses.RuleResponse;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.enumerator.RuleField;
import com.invest.domain.entities.enumerator.ComparisonOperator;
import com.invest.domain.entities.Rule;
import com.invest.domain.exceptions.AssetNotFoundException;
import com.invest.domain.exceptions.InvalidRuleFieldException;
import com.invest.domain.ports.out.repositories.AssetRepository;
import com.invest.domain.ports.out.repositories.RuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateRuleUseCaseImplTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private AssetRepository assetRepository;

    private CreateRuleUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateRuleUseCaseImpl(ruleRepository, assetRepository);
    }

    @Test
    void shouldCreateRuleWithValidData() {
        var command = new CreateRuleCommand("XPLG11", RuleField.PRICE, ComparisonOperator.LESS_THAN,
                BigDecimal.valueOf(100), null);
        var asset = new Asset(1L, "XPLG11", "FII XP Log", BigDecimal.valueOf(110),
                BigDecimal.valueOf(8.5), BigDecimal.valueOf(0.95), LocalDateTime.now());

        when(assetRepository.findByTicker("XPLG11")).thenReturn(Optional.of(asset));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> {
            Rule r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        RuleResponse response = useCase.execute(10L, command);

        assertEquals(1L, response.id());
        assertEquals("XPLG11", response.ticker());
        assertEquals(RuleField.PRICE, response.field());
        assertEquals(ComparisonOperator.LESS_THAN, response.operator());
        assertEquals(BigDecimal.valueOf(100), response.targetValue());
        assertTrue(response.active());
    }

    @Test
    void shouldSaveRuleWithCorrectUserId() {
        var command = new CreateRuleCommand("HGLG11", RuleField.DIVIDEND_YIELD, ComparisonOperator.GREATER_THAN,
                BigDecimal.valueOf(9), null);
        var asset = new Asset(2L, "HGLG11", "FII CSHG Log", BigDecimal.valueOf(160),
                BigDecimal.valueOf(9.2), BigDecimal.valueOf(1.1), LocalDateTime.now());

        when(assetRepository.findByTicker("HGLG11")).thenReturn(Optional.of(asset));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(42L, command);

        ArgumentCaptor<Rule> captor = ArgumentCaptor.forClass(Rule.class);
        verify(ruleRepository).save(captor.capture());
        assertEquals(42L, captor.getValue().getUserId());
    }

    @Test
    void shouldThrowAssetNotFoundException_whenTickerDoesNotExist() {
        var command = new CreateRuleCommand("INVALID", RuleField.P_VP, ComparisonOperator.EQUAL,
                BigDecimal.ONE, null);

        when(assetRepository.findByTicker("INVALID")).thenReturn(Optional.empty());

        assertThrows(AssetNotFoundException.class, () -> useCase.execute(1L, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenFieldIsNull() {
        var command = new CreateRuleCommand("XPLG11", null, ComparisonOperator.GREATER_THAN,
                BigDecimal.TEN, null);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(1L, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenOperatorIsNull() {
        var command = new CreateRuleCommand("XPLG11", RuleField.PRICE, null,
                BigDecimal.TEN, null);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(1L, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenTargetValueIsNull() {
        var command = new CreateRuleCommand("XPLG11", RuleField.PRICE, ComparisonOperator.GREATER_THAN,
                null, null);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(1L, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenTickerIsBlank() {
        var command = new CreateRuleCommand("  ", RuleField.PRICE, ComparisonOperator.GREATER_THAN,
                BigDecimal.TEN, null);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(1L, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldCreateRuleWithGroupId() {
        var command = new CreateRuleCommand("XPLG11", RuleField.P_VP, ComparisonOperator.LESS_THAN_OR_EQUAL,
                BigDecimal.valueOf(1.2), 5L);
        var asset = new Asset(1L, "XPLG11", "FII XP Log", BigDecimal.valueOf(110),
                BigDecimal.valueOf(8.5), BigDecimal.valueOf(0.95), LocalDateTime.now());

        when(assetRepository.findByTicker("XPLG11")).thenReturn(Optional.of(asset));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> {
            Rule r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        RuleResponse response = useCase.execute(1L, command);

        assertEquals(5L, response.groupId());
    }
}
