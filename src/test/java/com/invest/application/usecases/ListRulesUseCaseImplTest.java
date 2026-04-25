package com.invest.application.usecases;

import com.invest.application.responses.RuleResponse;
import com.invest.domain.entities.RuleField;
import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.Rule;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.RuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListRulesUseCaseImplTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private AlertRepository alertRepository;

    private ListRulesUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListRulesUseCaseImpl(ruleRepository, alertRepository);
    }

    @Test
    void shouldReturnOnlyRulesForAuthenticatedUser() {
        Long userId = 1L;
        var rule1 = new Rule(1L, userId, "XPLG11", null,
                RuleField.PRICE, ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(100),
                true, LocalDateTime.now(), LocalDateTime.now());
        var rule2 = new Rule(2L, userId, "HGLG11", null,
                RuleField.DIVIDEND_YIELD, ComparisonOperator.LESS_THAN, BigDecimal.valueOf(9),
                true, LocalDateTime.now(), LocalDateTime.now());

        when(ruleRepository.findByUserId(userId)).thenReturn(List.of(rule1, rule2));

        List<RuleResponse> result = useCase.execute(userId);

        assertEquals(2, result.size());
        assertEquals("XPLG11", result.get(0).ticker());
        assertEquals("HGLG11", result.get(1).ticker());
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoRules() {
        Long userId = 99L;

        when(ruleRepository.findByUserId(userId)).thenReturn(List.of());

        List<RuleResponse> result = useCase.execute(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldMapAllFieldsCorrectly() {
        Long userId = 1L;
        var rule = new Rule(5L, userId, "VISC11", 3L,
                RuleField.P_VP, ComparisonOperator.LESS_THAN_OR_EQUAL, BigDecimal.valueOf(1.05),
                true, LocalDateTime.now(), LocalDateTime.now());

        when(ruleRepository.findByUserId(userId)).thenReturn(List.of(rule));

        List<RuleResponse> result = useCase.execute(userId);

        assertEquals(1, result.size());
        RuleResponse response = result.getFirst();
        assertEquals(5L, response.id());
        assertEquals("VISC11", response.ticker());
        assertEquals(RuleField.P_VP, response.field());
        assertEquals(ComparisonOperator.LESS_THAN_OR_EQUAL, response.operator());
        assertEquals(BigDecimal.valueOf(1.05), response.targetValue());
        assertEquals(3L, response.groupId());
        assertTrue(response.active());
    }
}
