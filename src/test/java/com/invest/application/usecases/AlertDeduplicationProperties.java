package com.invest.application.usecases;

import com.invest.domain.entities.*;
import com.invest.domain.ports.out.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Validates: Requirement 5.5
 * Feature: investments-opportunity-monitor, Property 5: Prevencao de alertas duplicados
 */
class AlertDeduplicationProperties {

    private static final LocalDateTime NOW = LocalDateTime.now();

    @Provide
    Arbitrary<RuleField> fields() {
        return Arbitraries.of(RuleField.values());
    }

    @Provide
    Arbitrary<ComparisonOperator> operators() {
        return Arbitraries.of(ComparisonOperator.values());
    }

    @Provide
    Arbitrary<BigDecimal> values() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(100_000))
                .ofScale(2);
    }

    // Feature: investments-opportunity-monitor, Property 5: Prevencao de alertas duplicados
    @Property(tries = 200)
    void individualRule_doesNotCreateDuplicateAlert_whenActiveAlertExists(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("values") BigDecimal targetValue) {

        BigDecimal assetValue = computeSatisfyingValue(field, operator, targetValue);
        Asset asset = buildAsset(1L, "XPLG11", assetValue);
        Rule rule = new Rule(1L, 1L, "XPLG11", null, field, operator, targetValue, true, NOW, NOW);

        assert rule.evaluate(asset) : "Precondition failed: rule must be satisfied";

        AlertCapture capture = new AlertCapture();

        EvaluateRulesUseCaseImpl useCase = new EvaluateRulesUseCaseImpl(
                createRuleRepository(List.of(rule)),
                createRuleGroupRepository(List.of()),
                createAssetRepository(List.of(asset)),
                createAlertRepository(capture, Set.of(rule.getId()), Set.of())
        );

        useCase.execute();

        assert capture.savedAlerts.isEmpty() :
                "Expected no new alert when active alert already exists for rule %d, but %d were created"
                        .formatted(rule.getId(), capture.savedAlerts.size());
    }

    // Feature: investments-opportunity-monitor, Property 5: Prevencao de alertas duplicados
    @Property(tries = 200)
    void ruleGroup_doesNotCreateDuplicateAlert_whenActiveAlertExists(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("values") BigDecimal targetValue,
            @ForAll @IntRange(min = 1, max = 5) int ruleCount) {

        BigDecimal assetValue = computeSatisfyingValue(field, operator, targetValue);
        Asset asset = buildAsset(1L, "HGLG11", assetValue);

        List<Rule> rules = new ArrayList<>();
        for (int i = 0; i < ruleCount; i++) {
            rules.add(new Rule((long) (i + 1), 1L, "HGLG11", 100L, field, operator, targetValue, true, NOW, NOW));
        }

        RuleGroup group = new RuleGroup(100L, 1L, "HGLG11", "Test Group", rules, NOW);

        AlertCapture capture = new AlertCapture();

        EvaluateRulesUseCaseImpl useCase = new EvaluateRulesUseCaseImpl(
                createRuleRepository(rules),
                createRuleGroupRepository(List.of(group)),
                createAssetRepository(List.of(asset)),
                createAlertRepository(capture, Set.of(), Set.of(group.getId()))
        );

        useCase.execute();

        assert capture.savedAlerts.isEmpty() :
                "Expected no new alert when active alert already exists for group %d, but %d were created"
                        .formatted(group.getId(), capture.savedAlerts.size());
    }

    // Feature: investments-opportunity-monitor, Property 5: Prevencao de alertas duplicados
    @Property(tries = 200)
    void individualRule_createsExactlyOneAlert_whenNoActiveAlertExists(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("values") BigDecimal targetValue) {

        BigDecimal assetValue = computeSatisfyingValue(field, operator, targetValue);
        Asset asset = buildAsset(1L, "MXRF11", assetValue);
        Rule rule = new Rule(1L, 1L, "MXRF11", null, field, operator, targetValue, true, NOW, NOW);

        assert rule.evaluate(asset) : "Precondition failed: rule must be satisfied";

        AlertCapture capture = new AlertCapture();

        EvaluateRulesUseCaseImpl useCase = new EvaluateRulesUseCaseImpl(
                createRuleRepository(List.of(rule)),
                createRuleGroupRepository(List.of()),
                createAssetRepository(List.of(asset)),
                createAlertRepository(capture, Set.of(), Set.of())
        );

        useCase.execute();

        assert capture.savedAlerts.size() == 1 :
                "Expected exactly 1 alert when no active alert exists, but got %d"
                        .formatted(capture.savedAlerts.size());
    }

    // Feature: investments-opportunity-monitor, Property 5: Prevencao de alertas duplicados
    @Property(tries = 200)
    void multipleExecutions_neverCreateDuplicateAlerts(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("values") BigDecimal targetValue,
            @ForAll @IntRange(min = 2, max = 5) int executionCount) {

        BigDecimal assetValue = computeSatisfyingValue(field, operator, targetValue);
        Asset asset = buildAsset(1L, "VISC11", assetValue);
        Rule rule = new Rule(1L, 1L, "VISC11", null, field, operator, targetValue, true, NOW, NOW);

        assert rule.evaluate(asset) : "Precondition failed: rule must be satisfied";

        Set<Long> activeRuleAlerts = new HashSet<>();
        AlertCapture capture = new AlertCapture();

        for (int i = 0; i < executionCount; i++) {
            capture.savedAlerts.clear();

            EvaluateRulesUseCaseImpl useCase = new EvaluateRulesUseCaseImpl(
                    createRuleRepository(List.of(rule)),
                    createRuleGroupRepository(List.of()),
                    createAssetRepository(List.of(asset)),
                    createAlertRepository(capture, activeRuleAlerts, Set.of())
            );

            useCase.execute();

            if (i == 0) {
                assert capture.savedAlerts.size() == 1 :
                        "First execution should create exactly 1 alert, but got %d"
                                .formatted(capture.savedAlerts.size());
                activeRuleAlerts.add(rule.getId());
            } else {
                assert capture.savedAlerts.isEmpty() :
                        "Execution %d should not create alerts (active alert exists), but %d were created"
                                .formatted(i + 1, capture.savedAlerts.size());
            }
        }
    }

    private BigDecimal computeSatisfyingValue(RuleField field, ComparisonOperator operator, BigDecimal targetValue) {
        return switch (operator) {
            case GREATER_THAN -> targetValue.add(BigDecimal.ONE);
            case LESS_THAN -> targetValue.subtract(BigDecimal.ONE);
            case GREATER_THAN_OR_EQUAL, EQUAL -> targetValue;
            case LESS_THAN_OR_EQUAL -> targetValue;
        };
    }

    private Asset buildAsset(Long id, String ticker, BigDecimal value) {
        return new Asset(id, ticker, "Test Asset", value, value, value, NOW);
    }

    private static class AlertCapture {
        final List<Alert> savedAlerts = new ArrayList<>();
    }

    private RuleRepository createRuleRepository(List<Rule> rules) {
        return new RuleRepository() {
            @Override
            public Rule save(Rule regra) { return regra; }

            @Override
            public Optional<Rule> findById(Long regraId) {
                return rules.stream().filter(r -> r.getId().equals(regraId)).findFirst();
            }

            @Override
            public Optional<Rule> findByIdAndUserId(Long regraId, Long usuarioId) {
                return rules.stream()
                        .filter(r -> r.getId().equals(regraId) && r.getUserId().equals(usuarioId))
                        .findFirst();
            }

            @Override
            public List<Rule> findByUserId(Long usuarioId) {
                return rules.stream().filter(r -> r.getUserId().equals(usuarioId)).toList();
            }

            @Override
            public List<Rule> findAllActive() {
                return rules.stream().filter(Rule::isActive).toList();
            }

            @Override
            public List<Rule> findByGroupId(Long groupId) {
                return rules.stream().filter(r -> groupId.equals(r.getGroupId())).toList();
            }

            @Override
            public void delete(Long regraId) {}
        };
    }

    private RuleGroupRepository createRuleGroupRepository(List<RuleGroup> groups) {
        return new RuleGroupRepository() {
            @Override
            public RuleGroup save(RuleGroup grupo) { return grupo; }

            @Override
            public List<RuleGroup> findAllWithRules() { return groups; }

            @Override
            public List<RuleGroup> findByUserId(Long usuarioId) {
                return groups.stream().filter(g -> g.getUserId().equals(usuarioId)).toList();
            }
        };
    }

    private AssetRepository createAssetRepository(List<Asset> assets) {
        return new AssetRepository() {
            @Override
            public PageResult<Asset> findAll(PageRequest pageRequest) {
                return new PageResult<>(assets, 0, assets.size(), assets.size(), 1);
            }

            @Override
            public Optional<Asset> findByTicker(String ticker) {
                return assets.stream().filter(a -> a.getTicker().equals(ticker)).findFirst();
            }

            @Override
            public List<Asset> findByTickers(Set<String> tickers) {
                return assets.stream().filter(a -> tickers.contains(a.getTicker())).toList();
            }
        };
    }

    private AlertRepository createAlertRepository(AlertCapture capture,
                                                     Set<Long> activeRuleAlertIds,
                                                     Set<Long> activeGroupAlertIds) {
        return new AlertRepository() {
            @Override
            public Alert save(Alert alert) {
                capture.savedAlerts.add(alert);
                return alert;
            }

            @Override
            public PageResult<Alert> findByUserId(Long userId, PageRequest pageRequest) {
                return new PageResult<>(List.of(), 0, 10, 0, 0);
            }

            @Override
            public PageResult<Alert> findByUserIdAndTicker(Long userId, String ticker, PageRequest pageRequest) {
                return new PageResult<>(List.of(), 0, 10, 0, 0);
            }

            @Override
            public PageResult<Alert> findByUserIdAndStatus(Long userId, AlertStatus status, PageRequest pageRequest) {
                return new PageResult<>(List.of(), 0, 10, 0, 0);
            }

            @Override
            public PageResult<Alert> findByUserIdTickerAndStatus(Long userId, String ticker, AlertStatus status, PageRequest pageRequest) {
                return new PageResult<>(List.of(), 0, 10, 0, 0);
            }

            @Override
            public List<Alert> findPending() { return List.of(); }

            @Override
            public boolean existsActiveAlert(Long ruleId, String ticker) {
                return activeRuleAlertIds.contains(ruleId);
            }

            @Override
            public boolean existsActiveAlertForGroup(Long groupId, String ticker) {
                return activeGroupAlertIds.contains(groupId);
            }

            @Override
            public boolean existsByRuleId(Long ruleId) { return false; }
        };
    }
}
