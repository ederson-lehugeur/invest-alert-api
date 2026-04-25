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
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bug Condition Exploration - Property 1: Rule deactivation after successful alert send.
 *
 * These properties encode the EXPECTED behavior: after a PENDING alert is sent
 * successfully, the associated rule(s) must be deactivated (active=false).
 *
 * On UNFIXED code, these tests MUST FAIL - failure confirms the bug exists.
 * Do NOT fix the test or the code when it fails.
 */
class RuleDeactivationBugConditionProperties {

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

    // Property 1a - Bug Condition: Individual rule remains active after successful alert send
    // isBugCondition(input): alert.status=PENDING AND sendResult=SUCCESS AND alert.ruleId IS NOT NULL
    @Property(tries = 200)
    void individualRule_shouldBeDeactivated_afterSuccessfulAlertSend(
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

        RuleSaveCapture ruleSaveCapture = new RuleSaveCapture();

        SendPendingAlertsUseCaseImpl useCase = new SendPendingAlertsUseCaseImpl(
                createAlertRepository(List.of(pendingAlert)),
                createUserRepository(user),
                createAssetRepository(asset),
                createRuleRepository(List.of(rule), ruleSaveCapture),
                createRuleGroupRepository(List.of()),
                createSuccessEmailGateway(),
                new EmailContentBuilder()
        );

        useCase.execute();

        assert !rule.isActive() :
                "Bug confirmed: after successful alert send, rule %d remains active=true. Expected active=false."
                        .formatted(rule.getId());
    }

    // Property 1b - Bug Condition: All group rules remain active after successful alert send
    // isBugCondition(input): alert.status=PENDING AND sendResult=SUCCESS AND alert.groupId IS NOT NULL
    @Property(tries = 200)
    void ruleGroup_allRulesShouldBeDeactivated_afterSuccessfulAlertSend(
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

        RuleSaveCapture ruleSaveCapture = new RuleSaveCapture();

        SendPendingAlertsUseCaseImpl useCase = new SendPendingAlertsUseCaseImpl(
                createAlertRepository(List.of(pendingAlert)),
                createUserRepository(user),
                createAssetRepository(asset),
                createRuleRepository(rules, ruleSaveCapture),
                createRuleGroupRepository(List.of(group)),
                createSuccessEmailGateway(),
                new EmailContentBuilder()
        );

        useCase.execute();

        for (Rule rule : rules) {
            assert !rule.isActive() :
                    "Bug confirmed: after successful group alert send, rule %d in group %d remains active=true. Expected active=false."
                            .formatted(rule.getId(), groupId);
        }
    }

    // --- Helper classes and factory methods ---

    private static class RuleSaveCapture {
        final List<Rule> savedRules = new ArrayList<>();
    }

    private Asset buildAsset(Long id, String ticker, BigDecimal value) {
        return new Asset(id, ticker, "Test Asset", value, value, value, NOW);
    }

    private EmailGateway createSuccessEmailGateway() {
        return (recipient, subject, body) -> { /* successful send - no exception */ };
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
            public List<Asset> findByTickers(java.util.Set<String> tickers) {
                return tickers.contains(asset.getTicker()) ? List.of(asset) : List.of();
            }
        };
    }

    private RuleRepository createRuleRepository(List<Rule> rules, RuleSaveCapture capture) {
        return new RuleRepository() {
            @Override
            public Rule save(Rule rule) {
                capture.savedRules.add(rule);
                return rule;
            }

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
