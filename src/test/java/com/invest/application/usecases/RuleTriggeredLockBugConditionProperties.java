package com.invest.application.usecases;

import com.invest.application.commands.UpdateRuleCommand;
import com.invest.application.responses.RuleResponse;
import com.invest.domain.entities.Alert;
import com.invest.domain.entities.AlertStatus;
import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.Rule;
import com.invest.domain.entities.RuleField;
import com.invest.domain.exceptions.RuleAlreadyTriggeredException;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import com.invest.domain.ports.out.RuleRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bug Condition Exploration - Property 1: Lock DELETE/UPDATE on triggered rules.
 *
 * These properties encode the EXPECTED behavior: when a rule has associated alerts
 * (alertRepository.existsByRuleId(ruleId) = true), DELETE and UPDATE operations
 * must throw RuleAlreadyTriggeredException, and listing must return triggered=true.
 *
 * On UNFIXED code, these tests MUST FAIL - failure confirms the bug exists.
 * Do NOT fix the test or the code when it fails.
 *
 * Validates: Requirements 1.1, 1.2, 1.3, 2.1, 2.2, 2.3
 */
class RuleTriggeredLockBugConditionProperties {

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final Long USER_ID = 1L;
    private static final Long RULE_ID = 10L;
    private static final String TICKER = "PETR4";

    @Provide
    Arbitrary<RuleField> fields() {
        return Arbitraries.of(RuleField.values());
    }

    @Provide
    Arbitrary<ComparisonOperator> operators() {
        return Arbitraries.of(ComparisonOperator.values());
    }

    @Provide
    Arbitrary<BigDecimal> targetValues() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(100_000))
                .ofScale(2);
    }

    // Property 1a - Bug Condition: DELETE on a triggered rule must throw RuleAlreadyTriggeredException
    // isBugCondition(input): alertRepository.existsByRuleId(ruleId) = true AND operationType = DELETE
    // **Validates: Requirements 2.1**
    @Property(tries = 200)
    void deleteRule_shouldThrowRuleAlreadyTriggered_whenRuleHasAlerts(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("targetValues") BigDecimal targetValue) {

        Rule rule = new Rule(RULE_ID, USER_ID, TICKER, null, field, operator, targetValue, true, NOW, NOW);

        DeleteRuleUseCaseImpl useCase = new DeleteRuleUseCaseImpl(
                createRuleRepository(rule),
                createAlertRepositoryWithAlerts()
        );

        boolean threwExpectedException = false;
        try {
            useCase.execute(USER_ID, RULE_ID);
        } catch (RuleAlreadyTriggeredException e) {
            threwExpectedException = true;
        }

        assert threwExpectedException :
                "Bug confirmed: DeleteRuleUseCaseImpl.execute() did not throw RuleAlreadyTriggeredException "
                        + "for rule %d with field=%s, operator=%s, targetValue=%s. "
                        .formatted(RULE_ID, field, operator, targetValue)
                        + "Expected: RuleAlreadyTriggeredException. Actual: no exception (rule was deleted).";
    }

    // Property 1b - Bug Condition: UPDATE on a triggered rule must throw RuleAlreadyTriggeredException
    // isBugCondition(input): alertRepository.existsByRuleId(ruleId) = true AND operationType = UPDATE
    // **Validates: Requirements 2.2**
    @Property(tries = 200)
    void updateRule_shouldThrowRuleAlreadyTriggered_whenRuleHasAlerts(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("targetValues") BigDecimal targetValue) {

        Rule rule = new Rule(RULE_ID, USER_ID, TICKER, null, field, operator, targetValue, true, NOW, NOW);
        UpdateRuleCommand command = new UpdateRuleCommand(field, operator, targetValue);

        UpdateRuleUseCaseImpl useCase = new UpdateRuleUseCaseImpl(
                createRuleRepository(rule),
                createAlertRepositoryWithAlerts()
        );

        boolean threwExpectedException = false;
        try {
            useCase.execute(USER_ID, RULE_ID, command);
        } catch (RuleAlreadyTriggeredException e) {
            threwExpectedException = true;
        }

        assert threwExpectedException :
                "Bug confirmed: UpdateRuleUseCaseImpl.execute() did not throw RuleAlreadyTriggeredException "
                        + "for rule %d with field=%s, operator=%s, targetValue=%s. "
                        .formatted(RULE_ID, field, operator, targetValue)
                        + "Expected: RuleAlreadyTriggeredException. Actual: update was allowed.";
    }

    // Property 1c - Bug Condition: LIST must return triggered=true for rules with alerts
    // isBugCondition(input): alertRepository.existsByRuleId(ruleId) = true
    // **Validates: Requirements 2.3**
    @Property(tries = 200)
    void listRules_shouldReturnTriggeredTrue_whenRuleHasAlerts(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("targetValues") BigDecimal targetValue) {

        Rule rule = new Rule(RULE_ID, USER_ID, TICKER, null, field, operator, targetValue, true, NOW, NOW);

        ListRulesUseCaseImpl useCase = new ListRulesUseCaseImpl(
                createRuleRepository(rule),
                createAlertRepositoryWithAlerts()
        );

        List<RuleResponse> responses = useCase.execute(USER_ID);

        assert !responses.isEmpty() : "Expected at least one rule in response";

        RuleResponse response = responses.get(0);
        assert response.triggered() :
                "Bug confirmed: ListRulesUseCaseImpl.execute() returned triggered=false "
                        + "for rule %d with field=%s, operator=%s, targetValue=%s. "
                        .formatted(RULE_ID, field, operator, targetValue)
                        + "Expected: triggered=true (rule has associated alerts).";
    }

    // --- Helper factory methods (inline mocks following existing pattern) ---

    private RuleRepository createRuleRepository(Rule rule) {
        return new RuleRepository() {
            @Override
            public Rule save(Rule r) { return r; }

            @Override
            public Optional<Rule> findById(Long ruleId) {
                return rule.getId().equals(ruleId) ? Optional.of(rule) : Optional.empty();
            }

            @Override
            public Optional<Rule> findByIdAndUserId(Long ruleId, Long userId) {
                return rule.getId().equals(ruleId) && rule.getUserId().equals(userId)
                        ? Optional.of(rule) : Optional.empty();
            }

            @Override
            public List<Rule> findByUserId(Long userId) {
                return rule.getUserId().equals(userId) ? List.of(rule) : List.of();
            }

            @Override
            public List<Rule> findAllActive() {
                return rule.isActive() ? List.of(rule) : List.of();
            }

            @Override
            public List<Rule> findByGroupId(Long groupId) {
                return groupId != null && groupId.equals(rule.getGroupId()) ? List.of(rule) : List.of();
            }

            @Override
            public void delete(Long ruleId) {}
        };
    }

    private AlertRepository createAlertRepositoryWithAlerts() {
        return new AlertRepository() {
            @Override
            public Alert save(Alert alert) { return alert; }

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
            public boolean existsActiveAlert(Long ruleId, String ticker) { return false; }

            @Override
            public boolean existsActiveAlertForGroup(Long groupId, String ticker) { return false; }

            @Override
            public boolean existsByRuleId(Long ruleId) { return true; }
        };
    }
}
