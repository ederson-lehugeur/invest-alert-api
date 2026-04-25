package com.invest.application.usecases;

import com.invest.domain.entities.Alert;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.RuleField;
import com.invest.domain.entities.RuleGroup;
import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.Rule;
import com.invest.domain.entities.AlertStatus;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.AssetRepository;
import com.invest.domain.ports.out.RuleGroupRepository;
import com.invest.domain.ports.out.RuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluateRulesUseCaseImplTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private RuleGroupRepository ruleGroupRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AlertRepository alertRepository;

    private EvaluateRulesUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new EvaluateRulesUseCaseImpl(ruleRepository, ruleGroupRepository, assetRepository, alertRepository);
    }

    @Test
    void shouldCreateAlertWithStatusPending_whenIndividualRuleIsSatisfied() {
        Rule rule = buildIndividualRule(1L, 10L, "XPLG11", RuleField.PRICE,
                ComparisonOperator.LESS_THAN, BigDecimal.valueOf(120));
        Asset asset = buildAsset("XPLG11", BigDecimal.valueOf(100));

        when(ruleRepository.findAllActive()).thenReturn(List.of(rule));
        when(ruleGroupRepository.findAllWithRules()).thenReturn(List.of());
        when(assetRepository.findByTickers(Set.of("XPLG11"))).thenReturn(List.of(asset));
        when(alertRepository.existsActiveAlert(1L, "XPLG11")).thenReturn(false);

        useCase.execute();

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());

        Alert alert = captor.getValue();
        assertEquals(AlertStatus.PENDING, alert.getStatus());
        assertEquals(10L, alert.getUserId());
        assertEquals(1L, alert.getRuleId());
        assertNull(alert.getGroupId());
        assertEquals("XPLG11", alert.getTicker());
    }

    @Test
    void shouldNotCreateAlert_whenIndividualRuleIsNotSatisfied() {
        Rule rule = buildIndividualRule(1L, 10L, "XPLG11", RuleField.PRICE,
                ComparisonOperator.LESS_THAN, BigDecimal.valueOf(50));
        Asset asset = buildAsset("XPLG11", BigDecimal.valueOf(100));

        when(ruleRepository.findAllActive()).thenReturn(List.of(rule));
        when(ruleGroupRepository.findAllWithRules()).thenReturn(List.of());
        when(assetRepository.findByTickers(Set.of("XPLG11"))).thenReturn(List.of(asset));

        useCase.execute();

        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldContinueEvaluation_whenOneRuleThrowsException() {
        Rule faultyRule = buildIndividualRule(1L, 10L, "FAULTY", RuleField.PRICE,
                ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(50));
        Rule validRule = buildIndividualRule(2L, 10L, "XPLG11", RuleField.PRICE,
                ComparisonOperator.LESS_THAN, BigDecimal.valueOf(120));
        Asset asset = buildAsset("XPLG11", BigDecimal.valueOf(100));

        when(ruleRepository.findAllActive()).thenReturn(List.of(faultyRule, validRule));
        when(ruleGroupRepository.findAllWithRules()).thenReturn(List.of());
        when(assetRepository.findByTickers(Set.of("FAULTY", "XPLG11"))).thenReturn(List.of(asset));
        when(alertRepository.existsActiveAlert(2L, "XPLG11")).thenReturn(false);

        useCase.execute();

        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void shouldNotCreateDuplicateAlert_whenActiveAlertAlreadyExists() {
        Rule rule = buildIndividualRule(1L, 10L, "XPLG11", RuleField.PRICE,
                ComparisonOperator.LESS_THAN, BigDecimal.valueOf(120));
        Asset asset = buildAsset("XPLG11", BigDecimal.valueOf(100));

        when(ruleRepository.findAllActive()).thenReturn(List.of(rule));
        when(ruleGroupRepository.findAllWithRules()).thenReturn(List.of());
        when(assetRepository.findByTickers(Set.of("XPLG11"))).thenReturn(List.of(asset));
        when(alertRepository.existsActiveAlert(1L, "XPLG11")).thenReturn(true);

        useCase.execute();

        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldCreateAlertForGroup_whenAllGroupRulesAreSatisfied() {
        Rule rule1 = buildGroupRule(1L, 10L, "HGLG11", 100L, RuleField.PRICE,
                ComparisonOperator.LESS_THAN, BigDecimal.valueOf(200));
        Rule rule2 = buildGroupRule(2L, 10L, "HGLG11", 100L, RuleField.DIVIDEND_YIELD,
                ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(5));
        RuleGroup group = new RuleGroup(100L, 10L, "HGLG11", "Grupo FII",
                List.of(rule1, rule2), LocalDateTime.now());
        Asset asset = buildAsset("HGLG11", BigDecimal.valueOf(150));

        when(ruleRepository.findAllActive()).thenReturn(List.of(rule1, rule2));
        when(ruleGroupRepository.findAllWithRules()).thenReturn(List.of(group));
        when(assetRepository.findByTickers(Set.of("HGLG11"))).thenReturn(List.of(asset));
        when(alertRepository.existsActiveAlertForGroup(100L, "HGLG11")).thenReturn(false);

        useCase.execute();

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());

        Alert alert = captor.getValue();
        assertEquals(AlertStatus.PENDING, alert.getStatus());
        assertEquals(100L, alert.getGroupId());
        assertNull(alert.getRuleId());
    }

    @Test
    void shouldNotCreateAlertForGroup_whenNotAllGroupRulesAreSatisfied() {
        Rule rule1 = buildGroupRule(1L, 10L, "HGLG11", 100L, RuleField.PRICE,
                ComparisonOperator.LESS_THAN, BigDecimal.valueOf(200));
        Rule rule2 = buildGroupRule(2L, 10L, "HGLG11", 100L, RuleField.DIVIDEND_YIELD,
                ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(99));
        RuleGroup group = new RuleGroup(100L, 10L, "HGLG11", "Grupo FII",
                List.of(rule1, rule2), LocalDateTime.now());
        Asset asset = buildAsset("HGLG11", BigDecimal.valueOf(150));

        when(ruleRepository.findAllActive()).thenReturn(List.of(rule1, rule2));
        when(ruleGroupRepository.findAllWithRules()).thenReturn(List.of(group));
        when(assetRepository.findByTickers(Set.of("HGLG11"))).thenReturn(List.of(asset));

        useCase.execute();

        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldDoNothing_whenNoActiveRulesOrGroupsExist() {
        when(ruleRepository.findAllActive()).thenReturn(List.of());
        when(ruleGroupRepository.findAllWithRules()).thenReturn(List.of());

        useCase.execute();

        verify(assetRepository, never()).findByTickers(any());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldSkipRule_whenAssetNotFoundForTicker() {
        Rule rule = buildIndividualRule(1L, 10L, "MISSING", RuleField.PRICE,
                ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(50));

        when(ruleRepository.findAllActive()).thenReturn(List.of(rule));
        when(ruleGroupRepository.findAllWithRules()).thenReturn(List.of());
        when(assetRepository.findByTickers(Set.of("MISSING"))).thenReturn(List.of());

        useCase.execute();

        verify(alertRepository, never()).save(any());
    }

    private Rule buildIndividualRule(Long id, Long userId, String ticker,
                                      RuleField field, ComparisonOperator operator, BigDecimal targetValue) {
        return new Rule(id, userId, ticker, null, field, operator, targetValue,
                true, LocalDateTime.now(), LocalDateTime.now());
    }

    private Rule buildGroupRule(Long id, Long userId, String ticker, Long groupId,
                                  RuleField field, ComparisonOperator operator, BigDecimal targetValue) {
        return new Rule(id, userId, ticker, groupId, field, operator, targetValue,
                true, LocalDateTime.now(), LocalDateTime.now());
    }

    private Asset buildAsset(String ticker, BigDecimal price) {
        return new Asset(1L, ticker, "FII " + ticker, price,
                BigDecimal.valueOf(8.5), BigDecimal.valueOf(0.95), LocalDateTime.now());
    }
}
