package com.invest.application.usecases;

import com.invest.application.commands.UpdateRuleCommand;
import com.invest.application.responses.RuleResponse;
import com.invest.domain.entities.RuleField;
import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.Rule;
import com.invest.domain.exceptions.InvalidRuleFieldException;
import com.invest.domain.exceptions.RuleAlreadyTriggeredException;
import com.invest.domain.exceptions.RuleNotFoundException;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.RuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateRuleUseCaseImplTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private AlertRepository alertRepository;

    private UpdateRuleUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateRuleUseCaseImpl(ruleRepository, alertRepository);
    }

    @Test
    void shouldUpdateOwnRuleSuccessfully() {
        Long userId = 1L;
        Long ruleId = 10L;
        var existingRule = new Rule(ruleId, userId, "XPLG11", null,
                RuleField.PRICE, ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(100),
                true, LocalDateTime.now(), LocalDateTime.now());
        var command = new UpdateRuleCommand(RuleField.DIVIDEND_YIELD, ComparisonOperator.LESS_THAN,
                BigDecimal.valueOf(8));

        when(ruleRepository.findByIdAndUserId(ruleId, userId)).thenReturn(Optional.of(existingRule));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleResponse response = useCase.execute(userId, ruleId, command);

        assertEquals(RuleField.DIVIDEND_YIELD, response.field());
        assertEquals(ComparisonOperator.LESS_THAN, response.operator());
        assertEquals(BigDecimal.valueOf(8), response.targetValue());
        verify(ruleRepository).save(any(Rule.class));
    }

    @Test
    void shouldThrowRuleNotFoundException_whenRuleDoesNotBelongToUser() {
        Long userId = 1L;
        Long ruleId = 10L;
        var command = new UpdateRuleCommand(RuleField.PRICE, ComparisonOperator.GREATER_THAN,
                BigDecimal.TEN);

        when(ruleRepository.findByIdAndUserId(ruleId, userId)).thenReturn(Optional.empty());

        assertThrows(RuleNotFoundException.class, () -> useCase.execute(userId, ruleId, command));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenFieldIsNull() {
        var command = new UpdateRuleCommand(null, ComparisonOperator.GREATER_THAN, BigDecimal.TEN);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(1L, 10L, command));
        verify(ruleRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenOperatorIsNull() {
        var command = new UpdateRuleCommand(RuleField.PRICE, null, BigDecimal.TEN);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(1L, 10L, command));
        verify(ruleRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void shouldThrowInvalidRuleFieldException_whenTargetValueIsNull() {
        var command = new UpdateRuleCommand(RuleField.PRICE, ComparisonOperator.GREATER_THAN, null);

        assertThrows(InvalidRuleFieldException.class, () -> useCase.execute(1L, 10L, command));
        verify(ruleRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void shouldPreserveTickerAndGroupIdAfterUpdate() {
        Long userId = 1L;
        Long ruleId = 10L;
        var existingRule = new Rule(ruleId, userId, "HGLG11", 5L,
                RuleField.PRICE, ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(100),
                true, LocalDateTime.now(), LocalDateTime.now());
        var command = new UpdateRuleCommand(RuleField.P_VP, ComparisonOperator.EQUAL,
                BigDecimal.valueOf(1));

        when(ruleRepository.findByIdAndUserId(ruleId, userId)).thenReturn(Optional.of(existingRule));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleResponse response = useCase.execute(userId, ruleId, command);

        assertEquals("HGLG11", response.ticker());
        assertEquals(5L, response.groupId());
    }

    @Test
    void shouldThrowRuleAlreadyTriggeredException_whenRuleHasAlerts() {
        Long userId = 1L;
        Long ruleId = 10L;
        var existingRule = new Rule(ruleId, userId, "PETR4", null,
                RuleField.PRICE, ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(30),
                true, LocalDateTime.now(), LocalDateTime.now());
        var command = new UpdateRuleCommand(RuleField.DIVIDEND_YIELD, ComparisonOperator.LESS_THAN,
                BigDecimal.valueOf(8));

        when(ruleRepository.findByIdAndUserId(ruleId, userId)).thenReturn(Optional.of(existingRule));
        when(alertRepository.existsByRuleId(ruleId)).thenReturn(true);

        assertThrows(RuleAlreadyTriggeredException.class, () -> useCase.execute(userId, ruleId, command));
        verify(ruleRepository, never()).save(any());
    }
}
