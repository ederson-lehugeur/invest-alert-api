package com.invest.application.usecases;

import com.invest.application.commands.UpdateRuleCommand;
import com.invest.application.responses.RuleResponse;
import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.Rule;
import com.invest.domain.entities.RuleField;
import com.invest.domain.exceptions.RuleNotFoundException;
import com.invest.domain.entities.Alert;
import com.invest.domain.entities.AlertStatus;
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
 * Preservation Properties - Property 2: Unchanged behavior for rules WITHOUT alerts.
 *
 * These properties capture the baseline behavior of the UNFIXED code for inputs
 * where the bug condition does NOT apply (rules with no associated alerts).
 * They must PASS on unfixed code and continue to PASS after the fix is applied,
 * ensuring no regressions.
 *
 * Property 2a: DELETE on rules without alerts succeeds (no exception, delete called).
 * Property 2b: UPDATE on rules without alerts succeeds and returns correct RuleResponse.
 * Property 2c: LIST on rules without alerts returns triggered=false.
 *              NOTE: Will FAIL on unfixed code - triggered field doesn't exist yet.
 * Property 2d: DELETE/UPDATE on non-existent or other-user rules throws RuleNotFoundException.
 *
 * Validates: Requirements 3.1, 3.2, 3.4, 3.5
 */
class RuleTriggeredLockPreservationProperties {

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

    // --- Property 2a: DELETE on rules without alerts succeeds ---
    // Condition: NOT isBugCondition(input) - rule has no associated alerts
    // Observation: On current code, delete succeeds without exception and ruleRepository.delete() is called.
    // **Validates: Requirements 3.1**
    @Property(tries = 200)
    void deleteRule_shouldSucceed_whenRuleHasNoAlerts(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("targetValues") BigDecimal targetValue) {

        Rule rule = new Rule(RULE_ID, USER_ID, TICKER, null, field, operator, targetValue, true, NOW, NOW);
        TrackingRuleRepository ruleRepository = new TrackingRuleRepository(rule);

        DeleteRuleUseCaseImpl useCase = new DeleteRuleUseCaseImpl(ruleRepository, createAlertRepositoryWithoutAlerts());

        try {
            useCase.execute(USER_ID, RULE_ID);
        } catch (Exception e) {
            throw new AssertionError(
                    "Preservation violated: DeleteRuleUseCaseImpl.execute() threw %s for rule %d "
                            .formatted(e.getClass().getSimpleName(), RULE_ID)
                            + "with field=%s, operator=%s, targetValue=%s. "
                            .formatted(field, operator, targetValue)
                            + "Expected: successful deletion (no exception).", e);
        }

        assert ruleRepository.wasDeleteCalled() :
                "Preservation violated: ruleRepository.delete() was not called for rule %d "
                        .formatted(RULE_ID)
                        + "with field=%s, operator=%s, targetValue=%s."
                        .formatted(field, operator, targetValue);
    }

    // --- Property 2b: UPDATE on rules without alerts succeeds and returns correct RuleResponse ---
    // Condition: NOT isBugCondition(input) - rule has no associated alerts
    // Observation: On current code, update succeeds and returns RuleResponse with correct fields.
    // **Validates: Requirements 3.2, 3.4**
    @Property(tries = 200)
    void updateRule_shouldSucceedAndReturnCorrectResponse_whenRuleHasNoAlerts(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("targetValues") BigDecimal targetValue) {

        Long groupId = 5L;
        Rule rule = new Rule(RULE_ID, USER_ID, TICKER, groupId, field, operator, targetValue, true, NOW, NOW);
        RuleRepository ruleRepository = createRuleRepository(rule);
        UpdateRuleCommand command = new UpdateRuleCommand(field, operator, targetValue);

        UpdateRuleUseCaseImpl useCase = new UpdateRuleUseCaseImpl(ruleRepository, createAlertRepositoryWithoutAlerts());

        RuleResponse response;
        try {
            response = useCase.execute(USER_ID, RULE_ID, command);
        } catch (Exception e) {
            throw new AssertionError(
                    "Preservation violated: UpdateRuleUseCaseImpl.execute() threw %s for rule %d "
                            .formatted(e.getClass().getSimpleName(), RULE_ID)
                            + "with field=%s, operator=%s, targetValue=%s. "
                            .formatted(field, operator, targetValue)
                            + "Expected: successful update (no exception).", e);
        }

        assert response != null :
                "Preservation violated: UpdateRuleUseCaseImpl.execute() returned null for rule %d."
                        .formatted(RULE_ID);
        assert response.id().equals(RULE_ID) :
                "Preservation violated: response.id()=%d, expected=%d."
                        .formatted(response.id(), RULE_ID);
        assert response.ticker().equals(TICKER) :
                "Preservation violated: response.ticker()=%s, expected=%s."
                        .formatted(response.ticker(), TICKER);
        assert response.field() == field :
                "Preservation violated: response.field()=%s, expected=%s."
                        .formatted(response.field(), field);
        assert response.operator() == operator :
                "Preservation violated: response.operator()=%s, expected=%s."
                        .formatted(response.operator(), operator);
        assert response.targetValue().compareTo(targetValue) == 0 :
                "Preservation violated: response.targetValue()=%s, expected=%s."
                        .formatted(response.targetValue(), targetValue);
        assert response.groupId().equals(groupId) :
                "Preservation violated: response.groupId()=%s, expected=%s."
                        .formatted(response.groupId(), groupId);
        assert response.active() :
                "Preservation violated: response.active()=false, expected=true.";
    }

    // --- Property 2c: LIST on rules without alerts returns triggered=false ---
    // Condition: NOT isBugCondition(input) - rule has no associated alerts
    // **Validates: Requirements 3.4**
    @Property(tries = 200)
    void listRules_shouldReturnTriggeredFalse_whenRuleHasNoAlerts(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("targetValues") BigDecimal targetValue) {

        Rule rule = new Rule(RULE_ID, USER_ID, TICKER, null, field, operator, targetValue, true, NOW, NOW);
        RuleRepository ruleRepository = createRuleRepository(rule);

        AlertRepository alertRepository = createAlertRepositoryWithoutAlerts();
        ListRulesUseCaseImpl useCase = new ListRulesUseCaseImpl(ruleRepository, alertRepository);

        List<RuleResponse> responses = useCase.execute(USER_ID);

        assert !responses.isEmpty() : "Expected at least one rule in response";

        RuleResponse response = responses.get(0);
        assert !response.triggered() :
                "Preservation violated: ListRulesUseCaseImpl.execute() returned triggered=true "
                        + "for rule %d with no alerts. Expected: triggered=false."
                        .formatted(RULE_ID);
    }

    // --- Property 2d: DELETE/UPDATE on non-existent or other-user rules throws RuleNotFoundException ---
    // Condition: Rule does not exist or belongs to another user
    // Observation: On current code, both operations throw RuleNotFoundException.
    // **Validates: Requirements 3.5**
    @Property(tries = 200)
    void deleteRule_shouldThrowRuleNotFoundException_whenRuleDoesNotExist(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("targetValues") BigDecimal targetValue) {

        Rule rule = new Rule(RULE_ID, USER_ID, TICKER, null, field, operator, targetValue, true, NOW, NOW);
        RuleRepository ruleRepository = createRuleRepository(rule);

        DeleteRuleUseCaseImpl useCase = new DeleteRuleUseCaseImpl(ruleRepository, createAlertRepositoryWithoutAlerts());

        Long nonExistentRuleId = 999L;
        boolean threwExpectedException = false;
        try {
            useCase.execute(USER_ID, nonExistentRuleId);
        } catch (RuleNotFoundException e) {
            threwExpectedException = true;
        }

        assert threwExpectedException :
                "Preservation violated: DeleteRuleUseCaseImpl.execute() did not throw RuleNotFoundException "
                        + "for non-existent rule %d.".formatted(nonExistentRuleId);
    }

    @Property(tries = 200)
    void deleteRule_shouldThrowRuleNotFoundException_whenRuleBelongsToAnotherUser(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("targetValues") BigDecimal targetValue) {

        Rule rule = new Rule(RULE_ID, USER_ID, TICKER, null, field, operator, targetValue, true, NOW, NOW);
        RuleRepository ruleRepository = createRuleRepository(rule);

        DeleteRuleUseCaseImpl useCase = new DeleteRuleUseCaseImpl(ruleRepository, createAlertRepositoryWithoutAlerts());

        Long otherUserId = 99L;
        boolean threwExpectedException = false;
        try {
            useCase.execute(otherUserId, RULE_ID);
        } catch (RuleNotFoundException e) {
            threwExpectedException = true;
        }

        assert threwExpectedException :
                "Preservation violated: DeleteRuleUseCaseImpl.execute() did not throw RuleNotFoundException "
                        + "when user %d tried to delete rule %d belonging to user %d."
                        .formatted(otherUserId, RULE_ID, USER_ID);
    }

    @Property(tries = 200)
    void updateRule_shouldThrowRuleNotFoundException_whenRuleDoesNotExist(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("targetValues") BigDecimal targetValue) {

        Rule rule = new Rule(RULE_ID, USER_ID, TICKER, null, field, operator, targetValue, true, NOW, NOW);
        RuleRepository ruleRepository = createRuleRepository(rule);
        UpdateRuleCommand command = new UpdateRuleCommand(field, operator, targetValue);

        UpdateRuleUseCaseImpl useCase = new UpdateRuleUseCaseImpl(ruleRepository, createAlertRepositoryWithoutAlerts());

        Long nonExistentRuleId = 999L;
        boolean threwExpectedException = false;
        try {
            useCase.execute(USER_ID, nonExistentRuleId, command);
        } catch (RuleNotFoundException e) {
            threwExpectedException = true;
        }

        assert threwExpectedException :
                "Preservation violated: UpdateRuleUseCaseImpl.execute() did not throw RuleNotFoundException "
                        + "for non-existent rule %d.".formatted(nonExistentRuleId);
    }

    @Property(tries = 200)
    void updateRule_shouldThrowRuleNotFoundException_whenRuleBelongsToAnotherUser(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("targetValues") BigDecimal targetValue) {

        Rule rule = new Rule(RULE_ID, USER_ID, TICKER, null, field, operator, targetValue, true, NOW, NOW);
        RuleRepository ruleRepository = createRuleRepository(rule);
        UpdateRuleCommand command = new UpdateRuleCommand(field, operator, targetValue);

        UpdateRuleUseCaseImpl useCase = new UpdateRuleUseCaseImpl(ruleRepository, createAlertRepositoryWithoutAlerts());

        Long otherUserId = 99L;
        boolean threwExpectedException = false;
        try {
            useCase.execute(otherUserId, RULE_ID, command);
        } catch (RuleNotFoundException e) {
            threwExpectedException = true;
        }

        assert threwExpectedException :
                "Preservation violated: UpdateRuleUseCaseImpl.execute() did not throw RuleNotFoundException "
                        + "when user %d tried to update rule %d belonging to user %d."
                        .formatted(otherUserId, RULE_ID, USER_ID);
    }

    // --- Helper: RuleRepository with tracking for delete verification ---

    private static class TrackingRuleRepository implements RuleRepository {
        private final Rule rule;
        private boolean deleteCalled = false;

        TrackingRuleRepository(Rule rule) {
            this.rule = rule;
        }

        boolean wasDeleteCalled() {
            return deleteCalled;
        }

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
        public void delete(Long ruleId) {
            deleteCalled = true;
        }
    }

    // --- Helper: Standard RuleRepository (no tracking needed) ---

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

    // --- Helper: AlertRepository that returns no alerts (preservation scenario) ---

    private AlertRepository createAlertRepositoryWithoutAlerts() {
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
