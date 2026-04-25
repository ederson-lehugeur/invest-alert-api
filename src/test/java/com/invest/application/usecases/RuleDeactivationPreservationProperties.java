package com.invest.application.usecases;

import com.invest.application.EmailContentBuilder;
import com.invest.domain.entities.Alert;
import com.invest.domain.entities.AlertStatus;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.Rule;
import com.invest.domain.entities.RuleField;
import com.invest.domain.entities.RuleGroup;
import com.invest.domain.entities.User;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.AssetRepository;
import com.invest.domain.ports.out.EmailGateway;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import com.invest.domain.ports.out.RuleGroupRepository;
import com.invest.domain.ports.out.RuleRepository;
import com.invest.domain.ports.out.UserRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Preservation Properties - Property 2: Unchanged behavior for non-bug-condition scenarios.
 *
 * These properties capture the baseline behavior of the UNFIXED code for inputs
 * where the bug condition does NOT apply. They must PASS on unfixed code and
 * continue to PASS after the fix is applied, ensuring no regressions.
 *
 * Property 2a: When email send fails (exception), the rule stays active and alert stays PENDING.
 * Property 2b: Rules unrelated to the processed alert keep their active state unchanged.
 */
class RuleDeactivationPreservationProperties {

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final Long USER_ID = 1L;
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

    // --- Property 2a: Send failure preserves rule active state and alert PENDING status ---
    // Condition: NOT isBugCondition(input) where input.sendResult = FAILURE
    // Observation: On unfixed code, when emailGateway.send throws, rule stays active=true and alert stays PENDING.

    @Property(tries = 200)
    void individualRule_remainsActive_whenEmailSendFails(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("targetValues") BigDecimal targetValue) {

        Rule rule = new Rule(10L, USER_ID, TICKER, null, field, operator, targetValue, true, NOW, NOW);
        Asset asset = buildAsset(1L, TICKER, BigDecimal.valueOf(50));
        User user = new User(USER_ID, "Test User", "test@example.com", "hash", NOW, NOW);

        Alert pendingAlert = Alert.builder()
                .id(1L)
                .userId(USER_ID)
                .ruleId(rule.getId())
                .groupId(null)
                .ticker(TICKER)
                .status(AlertStatus.PENDING)
                .details("Test alert")
                .createdAt(NOW)
                .build();

        SendPendingAlertsUseCaseImpl useCase = new SendPendingAlertsUseCaseImpl(
                createAlertRepository(List.of(pendingAlert)),
                createUserRepository(user),
                createAssetRepository(asset),
                createRuleRepository(List.of(rule)),
                createRuleGroupRepository(List.of()),
                createFailingEmailGateway(),
                new EmailContentBuilder()
        );

        useCase.execute();

        assert rule.isActive() :
                "Preservation violated: rule %d should remain active=true when email send fails."
                        .formatted(rule.getId());
        assert pendingAlert.getStatus() == AlertStatus.PENDING :
                "Preservation violated: alert %d should remain PENDING when email send fails, but was %s."
                        .formatted(pendingAlert.getId(), pendingAlert.getStatus());
    }

    @Property(tries = 200)
    void ruleGroup_allRulesRemainActive_whenEmailSendFails(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("targetValues") BigDecimal targetValue,
            @ForAll @IntRange(min = 1, max = 5) int ruleCount) {

        Long groupId = 100L;
        List<Rule> rules = new ArrayList<>();
        for (int i = 0; i < ruleCount; i++) {
            rules.add(new Rule((long) (i + 1), USER_ID, TICKER, groupId, field, operator, targetValue, true, NOW, NOW));
        }

        RuleGroup group = new RuleGroup(groupId, USER_ID, TICKER, "Test Group", rules, NOW);
        Asset asset = buildAsset(1L, TICKER, BigDecimal.valueOf(50));
        User user = new User(USER_ID, "Test User", "test@example.com", "hash", NOW, NOW);

        Alert pendingAlert = Alert.builder()
                .id(2L)
                .userId(USER_ID)
                .ruleId(null)
                .groupId(groupId)
                .ticker(TICKER)
                .status(AlertStatus.PENDING)
                .details("Test group alert")
                .createdAt(NOW)
                .build();

        SendPendingAlertsUseCaseImpl useCase = new SendPendingAlertsUseCaseImpl(
                createAlertRepository(List.of(pendingAlert)),
                createUserRepository(user),
                createAssetRepository(asset),
                createRuleRepository(rules),
                createRuleGroupRepository(List.of(group)),
                createFailingEmailGateway(),
                new EmailContentBuilder()
        );

        useCase.execute();

        for (Rule rule : rules) {
            assert rule.isActive() :
                    "Preservation violated: rule %d in group %d should remain active=true when email send fails."
                            .formatted(rule.getId(), groupId);
        }
        assert pendingAlert.getStatus() == AlertStatus.PENDING :
                "Preservation violated: alert %d should remain PENDING when email send fails, but was %s."
                        .formatted(pendingAlert.getId(), pendingAlert.getStatus());
    }

    // --- Property 2b: Unrelated rules remain unchanged after successful alert send ---
    // Condition: isBugCondition(input) but for rules WHERE rule.id != alert.ruleId AND rule.groupId != alert.groupId
    // Observation: On unfixed code, rules not associated with the processed alert keep their active state.

    @Property(tries = 200)
    void unrelatedRules_remainUnchanged_afterSuccessfulIndividualAlertSend(
            @ForAll("fields") RuleField alertField,
            @ForAll("operators") ComparisonOperator alertOperator,
            @ForAll("targetValues") BigDecimal alertTargetValue,
            @ForAll @IntRange(min = 1, max = 5) int unrelatedCount) {

        // The rule associated with the alert
        Rule alertRule = new Rule(10L, USER_ID, TICKER, null, alertField, alertOperator, alertTargetValue, true, NOW, NOW);

        // Unrelated rules: different IDs, no group association
        List<Rule> unrelatedRules = new ArrayList<>();
        for (int i = 0; i < unrelatedCount; i++) {
            unrelatedRules.add(new Rule(
                    (long) (100 + i), USER_ID, "VALE3", null,
                    RuleField.PRICE, ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(80), true, NOW, NOW));
        }

        // Snapshot initial active states
        List<Boolean> initialStates = unrelatedRules.stream().map(Rule::isActive).toList();

        List<Rule> allRules = new ArrayList<>();
        allRules.add(alertRule);
        allRules.addAll(unrelatedRules);

        Asset asset = buildAsset(1L, TICKER, BigDecimal.valueOf(50));
        User user = new User(USER_ID, "Test User", "test@example.com", "hash", NOW, NOW);

        Alert pendingAlert = Alert.builder()
                .id(1L)
                .userId(USER_ID)
                .ruleId(alertRule.getId())
                .groupId(null)
                .ticker(TICKER)
                .status(AlertStatus.PENDING)
                .details("Test alert")
                .createdAt(NOW)
                .build();

        SendPendingAlertsUseCaseImpl useCase = new SendPendingAlertsUseCaseImpl(
                createAlertRepository(List.of(pendingAlert)),
                createUserRepository(user),
                createAssetRepository(asset),
                createRuleRepository(allRules),
                createRuleGroupRepository(List.of()),
                createSuccessEmailGateway(),
                new EmailContentBuilder()
        );

        useCase.execute();

        for (int i = 0; i < unrelatedRules.size(); i++) {
            Rule unrelated = unrelatedRules.get(i);
            boolean expected = initialStates.get(i);
            assert unrelated.isActive() == expected :
                    "Preservation violated: unrelated rule %d active state changed from %s to %s after alert send for rule %d."
                            .formatted(unrelated.getId(), expected, unrelated.isActive(), alertRule.getId());
        }
    }

    @Property(tries = 200)
    void unrelatedRules_remainUnchanged_afterSuccessfulGroupAlertSend(
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator,
            @ForAll("targetValues") BigDecimal targetValue,
            @ForAll @IntRange(min = 1, max = 3) int groupRuleCount,
            @ForAll @IntRange(min = 1, max = 5) int unrelatedCount) {

        Long groupId = 100L;

        // Rules belonging to the alert's group
        List<Rule> groupRules = new ArrayList<>();
        for (int i = 0; i < groupRuleCount; i++) {
            groupRules.add(new Rule((long) (i + 1), USER_ID, TICKER, groupId, field, operator, targetValue, true, NOW, NOW));
        }

        // Unrelated rules: different IDs, different group (null)
        List<Rule> unrelatedRules = new ArrayList<>();
        for (int i = 0; i < unrelatedCount; i++) {
            unrelatedRules.add(new Rule(
                    (long) (200 + i), USER_ID, "VALE3", null,
                    RuleField.PRICE, ComparisonOperator.GREATER_THAN, BigDecimal.valueOf(80), true, NOW, NOW));
        }

        List<Boolean> initialStates = unrelatedRules.stream().map(Rule::isActive).toList();

        List<Rule> allRules = new ArrayList<>();
        allRules.addAll(groupRules);
        allRules.addAll(unrelatedRules);

        RuleGroup group = new RuleGroup(groupId, USER_ID, TICKER, "Test Group", groupRules, NOW);
        Asset asset = buildAsset(1L, TICKER, BigDecimal.valueOf(50));
        User user = new User(USER_ID, "Test User", "test@example.com", "hash", NOW, NOW);

        Alert pendingAlert = Alert.builder()
                .id(2L)
                .userId(USER_ID)
                .ruleId(null)
                .groupId(groupId)
                .ticker(TICKER)
                .status(AlertStatus.PENDING)
                .details("Test group alert")
                .createdAt(NOW)
                .build();

        SendPendingAlertsUseCaseImpl useCase = new SendPendingAlertsUseCaseImpl(
                createAlertRepository(List.of(pendingAlert)),
                createUserRepository(user),
                createAssetRepository(asset),
                createRuleRepository(allRules),
                createRuleGroupRepository(List.of(group)),
                createSuccessEmailGateway(),
                new EmailContentBuilder()
        );

        useCase.execute();

        for (int i = 0; i < unrelatedRules.size(); i++) {
            Rule unrelated = unrelatedRules.get(i);
            boolean expected = initialStates.get(i);
            assert unrelated.isActive() == expected :
                    "Preservation violated: unrelated rule %d active state changed from %s to %s after group alert send for group %d."
                            .formatted(unrelated.getId(), expected, unrelated.isActive(), groupId);
        }
    }

    // --- Helper factory methods (inline stubs following project pattern) ---

    private Asset buildAsset(Long id, String ticker, BigDecimal value) {
        return new Asset(id, ticker, "Test Asset", value, value, value, NOW);
    }

    private EmailGateway createSuccessEmailGateway() {
        return (recipient, subject, body) -> { /* successful send */ };
    }

    private EmailGateway createFailingEmailGateway() {
        return (recipient, subject, body) -> {
            throw new RuntimeException("SMTP connection failed");
        };
    }

    private UserRepository createUserRepository(User user) {
        return new UserRepository() {
            @Override
            public User save(User u) { return u; }

            @Override
            public Optional<User> findById(Long id) {
                return user.getId().equals(id) ? Optional.of(user) : Optional.empty();
            }

            @Override
            public Optional<User> findByEmail(String email) {
                return user.getEmail().equals(email) ? Optional.of(user) : Optional.empty();
            }

            @Override
            public boolean existsByEmail(String email) {
                return user.getEmail().equals(email);
            }
        };
    }

    private AssetRepository createAssetRepository(Asset asset) {
        return new AssetRepository() {
            @Override
            public PageResult<Asset> findAll(PageRequest pageRequest) {
                return new PageResult<>(List.of(asset), 0, 1, 1, 1);
            }

            @Override
            public Optional<Asset> findByTicker(String ticker) {
                return asset.getTicker().equals(ticker) ? Optional.of(asset) : Optional.empty();
            }

            @Override
            public List<Asset> findByTickers(Set<String> tickers) {
                return tickers.contains(asset.getTicker()) ? List.of(asset) : List.of();
            }
        };
    }

    private RuleRepository createRuleRepository(List<Rule> rules) {
        return new RuleRepository() {
            @Override
            public Rule save(Rule rule) { return rule; }

            @Override
            public Optional<Rule> findById(Long ruleId) {
                return rules.stream().filter(r -> r.getId().equals(ruleId)).findFirst();
            }

            @Override
            public Optional<Rule> findByIdAndUserId(Long ruleId, Long userId) {
                return rules.stream()
                        .filter(r -> r.getId().equals(ruleId) && r.getUserId().equals(userId))
                        .findFirst();
            }

            @Override
            public List<Rule> findByUserId(Long userId) {
                return rules.stream().filter(r -> r.getUserId().equals(userId)).toList();
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
            public void delete(Long ruleId) {}
        };
    }

    private RuleGroupRepository createRuleGroupRepository(List<RuleGroup> groups) {
        return new RuleGroupRepository() {
            @Override
            public RuleGroup save(RuleGroup group) { return group; }

            @Override
            public List<RuleGroup> findAllWithRules() { return groups; }

            @Override
            public List<RuleGroup> findByUserId(Long userId) {
                return groups.stream().filter(g -> g.getUserId().equals(userId)).toList();
            }
        };
    }

    private AlertRepository createAlertRepository(List<Alert> pendingAlerts) {
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
            public List<Alert> findPending() { return new ArrayList<>(pendingAlerts); }

            @Override
            public boolean existsActiveAlert(Long ruleId, String ticker) { return false; }

            @Override
            public boolean existsActiveAlertForGroup(Long groupId, String ticker) { return false; }

            @Override
            public boolean existsByRuleId(Long ruleId) { return false; }
        };
    }
}
