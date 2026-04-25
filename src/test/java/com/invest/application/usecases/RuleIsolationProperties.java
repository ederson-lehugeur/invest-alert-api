package com.invest.application.usecases;

import com.invest.application.responses.RuleResponse;
import com.invest.domain.entities.Alert;
import com.invest.domain.entities.AlertStatus;
import com.invest.domain.entities.RuleField;
import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.Rule;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import com.invest.domain.ports.out.RuleRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Validates: Requirement 3.4
 * Feature: investments-opportunity-monitor, Property 3: Isolamento de dados de regras por usuario
 */
class RuleIsolationProperties {

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

    // Feature: investments-opportunity-monitor, Property 3: Isolamento de dados de regras por usuario
    @Property(tries = 200)
    void listRules_returnsOnlyRulesForRequestedUser(
            @ForAll @IntRange(min = 2, max = 5) int userCount,
            @ForAll @IntRange(min = 1, max = 8) int rulesPerUser,
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("values") BigDecimal value) {

        List<Rule> allRules = new ArrayList<>();
        long ruleIdCounter = 1L;

        for (long userId = 1; userId <= userCount; userId++) {
            for (int r = 0; r < rulesPerUser; r++) {
                allRules.add(new Rule(
                        ruleIdCounter++, userId, "XPLG11", null,
                        field, operator, value, true, NOW, NOW
                ));
            }
        }

        RuleRepository stubRepository = createStubRepository(allRules);
        ListRulesUseCaseImpl useCase = new ListRulesUseCaseImpl(stubRepository, createNoAlertsRepository());

        for (long targetUserId = 1; targetUserId <= userCount; targetUserId++) {
            List<RuleResponse> result = useCase.execute(targetUserId);

            long finalTargetUserId = targetUserId;
            boolean allBelongToUser = result.stream()
                    .allMatch(response -> {
                        Rule original = allRules.stream()
                                .filter(rule -> rule.getId().equals(response.id()))
                                .findFirst()
                                .orElseThrow();
                        return original.getUserId().equals(finalTargetUserId);
                    });

            assert allBelongToUser :
                    "ListRules returned rules not belonging to user %d".formatted(targetUserId);

            assert result.size() == rulesPerUser :
                    "Expected %d rules for user %d but got %d"
                            .formatted(rulesPerUser, targetUserId, result.size());
        }
    }

    // Feature: investments-opportunity-monitor, Property 3: Isolamento de dados de regras por usuario
    @Property(tries = 100)
    void listRules_returnsEmptyForUserWithNoRules(
            @ForAll @IntRange(min = 1, max = 5) int rulesForOtherUser,
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("values") BigDecimal value) {

        Long ownerUserId = 1L;
        Long queryUserId = 99L;

        List<Rule> allRules = new ArrayList<>();
        for (int i = 0; i < rulesForOtherUser; i++) {
            allRules.add(new Rule(
                    (long) (i + 1), ownerUserId, "HGLG11", null,
                    field, operator, value, true, NOW, NOW
            ));
        }

        RuleRepository stubRepository = createStubRepository(allRules);
        ListRulesUseCaseImpl useCase = new ListRulesUseCaseImpl(stubRepository, createNoAlertsRepository());

        List<RuleResponse> result = useCase.execute(queryUserId);

        assert result.isEmpty() :
                "Expected empty list for user %d but got %d rules".formatted(queryUserId, result.size());
    }

    private RuleRepository createStubRepository(List<Rule> allRules) {
        return new RuleRepository() {
            @Override
            public Rule save(Rule regra) { return regra; }

            @Override
            public Optional<Rule> findById(Long regraId) {
                return allRules.stream().filter(r -> r.getId().equals(regraId)).findFirst();
            }

            @Override
            public Optional<Rule> findByIdAndUserId(Long regraId, Long usuarioId) {
                return allRules.stream()
                        .filter(r -> r.getId().equals(regraId) && r.getUserId().equals(usuarioId))
                        .findFirst();
            }

            @Override
            public List<Rule> findByUserId(Long usuarioId) {
                return allRules.stream()
                        .filter(r -> r.getUserId().equals(usuarioId))
                        .toList();
            }

            @Override
            public List<Rule> findAllActive() { return allRules; }

            @Override
            public List<Rule> findByGroupId(Long groupId) {
                return allRules.stream().filter(r -> groupId.equals(r.getGroupId())).toList();
            }

            @Override
            public void delete(Long regraId) {}
        };
    }

    private AlertRepository createNoAlertsRepository() {
        return new AlertRepository() {
            @Override
            public Alert save(Alert alert) { return alert; }

            @Override
            public PageResult<Alert> findByUserId(Long userId, PageRequest pageRequest) { return null; }

            @Override
            public PageResult<Alert> findByUserIdAndTicker(Long userId, String ticker, PageRequest pageRequest) { return null; }

            @Override
            public PageResult<Alert> findByUserIdAndStatus(Long userId, AlertStatus status, PageRequest pageRequest) { return null; }

            @Override
            public PageResult<Alert> findByUserIdTickerAndStatus(Long userId, String ticker, AlertStatus status, PageRequest pageRequest) { return null; }

            @Override
            public List<Alert> findPending() { return List.of(); }

            @Override
            public boolean existsByRuleId(Long ruleId) { return false; }

            @Override
            public boolean existsActiveAlert(Long ruleId, String ticker) { return false; }

            @Override
            public boolean existsActiveAlertForGroup(Long groupId, String ticker) { return false; }
        };
    }
}
