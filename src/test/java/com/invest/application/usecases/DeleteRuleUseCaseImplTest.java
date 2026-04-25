package com.invest.application.usecases;

import com.invest.domain.entities.RuleField;
import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.Rule;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteRuleUseCaseImplTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private AlertRepository alertRepository;

    private DeleteRuleUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteRuleUseCaseImpl(ruleRepository, alertRepository);
    }

    @Test
    void shouldDeleteOwnRuleSuccessfully() {
        Long userId = 1L;
        Long ruleId = 10L;
        var rule = new Rule(ruleId, userId, "XPLG11", null,
                RuleField.PRICE, ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(100),
                true, LocalDateTime.now(), LocalDateTime.now());

        when(ruleRepository.findByIdAndUserId(ruleId, userId)).thenReturn(Optional.of(rule));
        when(alertRepository.existsByRuleId(ruleId)).thenReturn(false);

        useCase.execute(userId, ruleId);

        verify(ruleRepository).delete(ruleId);
    }

    @Test
    void shouldThrowRuleNotFoundException_whenRuleDoesNotBelongToUser() {
        Long userId = 1L;
        Long ruleId = 10L;

        when(ruleRepository.findByIdAndUserId(ruleId, userId)).thenReturn(Optional.empty());

        assertThrows(RuleNotFoundException.class, () -> useCase.execute(userId, ruleId));
        verify(ruleRepository, never()).delete(any());
    }

    @Test
    void shouldThrowRuleNotFoundException_whenRuleBelongsToAnotherUser() {
        Long ownerUserId = 1L;
        Long attackerUserId = 2L;
        Long ruleId = 10L;

        when(ruleRepository.findByIdAndUserId(ruleId, attackerUserId)).thenReturn(Optional.empty());

        assertThrows(RuleNotFoundException.class, () -> useCase.execute(attackerUserId, ruleId));
        verify(ruleRepository, never()).delete(any());
    }
}
