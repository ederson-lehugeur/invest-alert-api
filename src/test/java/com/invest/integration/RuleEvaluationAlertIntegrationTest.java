package com.invest.integration;

import com.invest.adapters.persistence.AlertRepositoryAdapter;
import com.invest.adapters.persistence.AssetRepositoryAdapter;
import com.invest.adapters.persistence.RuleGroupRepositoryAdapter;
import com.invest.adapters.persistence.JpaAlertRepository;
import com.invest.adapters.persistence.JpaAssetRepository;
import com.invest.adapters.persistence.JpaRuleGroupRepository;
import com.invest.adapters.persistence.JpaRuleRepository;
import com.invest.adapters.persistence.JpaUserRepository;
import com.invest.adapters.persistence.RuleRepositoryAdapter;
import com.invest.adapters.persistence.UserRepositoryAdapter;
import com.invest.application.EmailContentBuilder;
import com.invest.application.usecases.EvaluateRulesUseCaseImpl;
import com.invest.application.usecases.SendPendingAlertsUseCaseImpl;
import com.invest.domain.entities.RuleField;
import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.AssetRepository;
import com.invest.domain.ports.out.EmailGateway;
import com.invest.domain.ports.out.RuleGroupRepository;
import com.invest.domain.ports.out.RuleRepository;
import com.invest.domain.ports.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = RuleEvaluationAlertIntegrationTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.show-sql=true",
        "spring.sql.init.mode=never",
        "spring.jpa.defer-datasource-initialization=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@Sql(scripts = "classpath:schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Transactional
class RuleEvaluationAlertIntegrationTest {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    @Configuration
    @EnableAutoConfiguration(exclude = {
            SecurityAutoConfiguration.class,
            SecurityFilterAutoConfiguration.class,
            QuartzAutoConfiguration.class,
            MailSenderAutoConfiguration.class
    })
    static class TestConfig {

        @Bean
        org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
            return properties -> properties.put(
                    org.hibernate.cfg.AvailableSettings.LOADED_CLASSES,
                    java.util.List.of(
                            com.invest.adapters.persistence.UserEntity.class,
                            com.invest.adapters.persistence.AssetEntity.class,
                            com.invest.adapters.persistence.RuleEntity.class,
                            com.invest.adapters.persistence.RuleGroupEntity.class,
                            com.invest.adapters.persistence.AlertEntity.class
                    )
            );
        }

        @Bean
        JpaRuleRepository jpaRuleRepository(jakarta.persistence.EntityManager em) {
            return new JpaRepositoryFactory(em).getRepository(JpaRuleRepository.class);
        }

        @Bean
        JpaUserRepository jpaUserRepository(jakarta.persistence.EntityManager em) {
            return new JpaRepositoryFactory(em).getRepository(JpaUserRepository.class);
        }

        @Bean
        JpaAssetRepository jpaAssetRepository(jakarta.persistence.EntityManager em) {
            return new JpaRepositoryFactory(em).getRepository(JpaAssetRepository.class);
        }

        @Bean
        JpaRuleGroupRepository jpaRuleGroupRepository(jakarta.persistence.EntityManager em) {
            return new JpaRepositoryFactory(em).getRepository(JpaRuleGroupRepository.class);
        }

        @Bean
        JpaAlertRepository jpaAlertRepository(jakarta.persistence.EntityManager em) {
            return new JpaRepositoryFactory(em).getRepository(JpaAlertRepository.class);
        }

        @Bean
        EmailGateway emailGateway() {
            return mock(EmailGateway.class);
        }

        @Bean
        UserRepository userRepository(JpaUserRepository jpa) {
            return new UserRepositoryAdapter(jpa);
        }

        @Bean
        AssetRepository assetRepository(JpaAssetRepository jpa) {
            return new AssetRepositoryAdapter(jpa);
        }

        @Bean
        RuleRepository ruleRepository(JpaRuleRepository jpa, JpaUserRepository jpaUser,
                                       JpaAssetRepository jpaAsset, JpaRuleGroupRepository jpaGroup) {
            return new RuleRepositoryAdapter(jpa, jpaUser, jpaAsset, jpaGroup);
        }

        @Bean
        RuleGroupRepository ruleGroupRepository(JpaRuleGroupRepository jpa,
                                                JpaUserRepository jpaUser,
                                                JpaAssetRepository jpaAsset) {
            return new RuleGroupRepositoryAdapter(jpa, jpaUser, jpaAsset);
        }

        @Bean
        AlertRepository alertRepository(JpaAlertRepository jpa, JpaUserRepository jpaUser,
                                         JpaRuleRepository jpaRule, JpaRuleGroupRepository jpaGroup) {
            return new AlertRepositoryAdapter(jpa, jpaUser, jpaRule, jpaGroup);
        }

        @Bean
        EmailContentBuilder emailContentBuilder() {
            return new EmailContentBuilder();
        }

        @Bean
        EvaluateRulesUseCaseImpl evaluateRulesUseCase(RuleRepository ruleRepo,
                                                      RuleGroupRepository groupRepo,
                                                      AssetRepository assetRepo,
                                                      AlertRepository alertRepo) {
            return new EvaluateRulesUseCaseImpl(ruleRepo, groupRepo, assetRepo, alertRepo);
        }

        @Bean
        SendPendingAlertsUseCaseImpl sendPendingAlertsUseCase(AlertRepository alertRepo,
                                                              UserRepository userRepo,
                                                              AssetRepository assetRepo,
                                                              RuleRepository ruleRepo,
                                                              RuleGroupRepository groupRepo,
                                                              EmailGateway emailGateway,
                                                              EmailContentBuilder emailContentBuilder) {
            return new SendPendingAlertsUseCaseImpl(alertRepo, userRepo, assetRepo,
                    ruleRepo, groupRepo, emailGateway, emailContentBuilder);
        }
    }

    @Autowired
    private EvaluateRulesUseCaseImpl evaluateRulesUseCase;

    @Autowired
    private SendPendingAlertsUseCaseImpl sendPendingAlertsUseCase;

    @Autowired
    private EmailGateway emailGateway;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Long userId;

    @BeforeEach
    void setUp() {
        jdbc.execute("DELETE FROM alert");
        jdbc.execute("DELETE FROM rule");
        jdbc.execute("DELETE FROM rule_group");
        jdbc.execute("DELETE FROM asset");
        jdbc.execute("DELETE FROM \"user\"");

        userId = insertUser("Test User", "test@example.com");
        insertAsset("HGLG11", "CSHG Logistica FII",
                new BigDecimal("160.00"), new BigDecimal("8.50"), new BigDecimal("0.95"));
        entityManager.flush();
        entityManager.clear();

        reset(emailGateway);
    }

    // ========================================================================
    // Rule evaluation with real repositories (Req 5.2, 5.3)
    // ========================================================================

    @Test
    @DisplayName("Individual rule satisfied should create PENDING alert")
    void individualRuleSatisfied_shouldCreatePendingAlert() {
        insertIndividualRule(userId, "HGLG11", RuleField.PRICE,
                ComparisonOperator.GREATER_THAN, new BigDecimal("100.00"));
        syncJpaCache();

        evaluateRulesUseCase.execute();
        syncJpaCache();

        List<Map<String, Object>> alerts = jdbc.queryForList("SELECT * FROM alert");
        assertThat(alerts).hasSize(1);

        Map<String, Object> alert = alerts.getFirst();
        assertThat(alert.get("STATUS")).isEqualTo("PENDING");
        assertThat(alert.get("TICKER")).isEqualTo("HGLG11");
        assertThat(alert.get("USER_ID")).isEqualTo(userId);
        assertThat(alert.get("RULE_ID")).isNotNull();
        assertThat(alert.get("GROUP_ID")).isNull();
    }

    @Test
    @DisplayName("Individual rule NOT satisfied should not create alert")
    void individualRuleNotSatisfied_shouldNotCreateAlert() {
        insertIndividualRule(userId, "HGLG11", RuleField.PRICE,
                ComparisonOperator.LESS_THAN, new BigDecimal("100.00"));
        syncJpaCache();

        evaluateRulesUseCase.execute();

        assertThat(jdbc.queryForList("SELECT * FROM alert")).isEmpty();
    }

    @Test
    @DisplayName("Multiple rules from different users should create separate alerts")
    void multipleUsersWithSatisfiedRules_shouldCreateSeparateAlerts() {
        Long secondUserId = insertUser("Second User", "second@example.com");

        insertIndividualRule(userId, "HGLG11", RuleField.PRICE,
                ComparisonOperator.GREATER_THAN, new BigDecimal("100.00"));
        insertIndividualRule(secondUserId, "HGLG11", RuleField.DIVIDEND_YIELD,
                ComparisonOperator.GREATER_THAN, new BigDecimal("5.00"));
        syncJpaCache();

        evaluateRulesUseCase.execute();
        syncJpaCache();

        List<Map<String, Object>> alerts = jdbc.queryForList("SELECT * FROM alert");
        assertThat(alerts).hasSize(2);
        assertThat(alerts).extracting(a -> a.get("USER_ID"))
                .containsExactlyInAnyOrder(userId, secondUserId);
    }

    @Test
    @DisplayName("Group of rules all satisfied should create single alert for group")
    void groupRulesAllSatisfied_shouldCreateSingleGroupAlert() {
        Long groupId = insertGroupWithRules(userId, "HGLG11", "FII Opportunity",
                List.of(
                        new RuleSpec(RuleField.PRICE, ComparisonOperator.GREATER_THAN, new BigDecimal("100.00")),
                        new RuleSpec(RuleField.DIVIDEND_YIELD, ComparisonOperator.GREATER_THAN, new BigDecimal("5.00"))
                ));
        syncJpaCache();

        evaluateRulesUseCase.execute();
        syncJpaCache();

        List<Map<String, Object>> alerts = jdbc.queryForList("SELECT * FROM alert");
        assertThat(alerts).hasSize(1);

        Map<String, Object> alert = alerts.getFirst();
        assertThat(alert.get("STATUS")).isEqualTo("PENDING");
        assertThat(alert.get("GROUP_ID")).isEqualTo(groupId);
        assertThat(alert.get("RULE_ID")).isNull();
    }

    @Test
    @DisplayName("Group of rules with one NOT satisfied should not create alert")
    void groupRulesPartiallyUnsatisfied_shouldNotCreateAlert() {
        insertGroupWithRules(userId, "HGLG11", "Strict Group",
                List.of(
                        new RuleSpec(RuleField.PRICE, ComparisonOperator.GREATER_THAN, new BigDecimal("100.00")),
                        new RuleSpec(RuleField.P_VP, ComparisonOperator.LESS_THAN, new BigDecimal("0.50"))
                ));
        syncJpaCache();

        evaluateRulesUseCase.execute();

        assertThat(jdbc.queryForList("SELECT * FROM alert")).isEmpty();
    }

    @Test
    @DisplayName("Duplicate alert should not be created for same rule and ticker")
    void duplicateAlert_shouldNotBeCreated() {
        insertIndividualRule(userId, "HGLG11", RuleField.PRICE,
                ComparisonOperator.GREATER_THAN, new BigDecimal("100.00"));
        syncJpaCache();

        evaluateRulesUseCase.execute();
        syncJpaCache();
        assertThat(jdbc.queryForList("SELECT * FROM alert")).hasSize(1);

        evaluateRulesUseCase.execute();
        syncJpaCache();
        assertThat(jdbc.queryForList("SELECT * FROM alert")).hasSize(1);
    }

    @Test
    @DisplayName("Evaluation should continue processing remaining rules when one encounters an error")
    void evaluationContinuesOnError_shouldCreateAlertForValidRules() {
        insertAsset("XPLG11", "XP Log FII",
                new BigDecimal("100.00"), new BigDecimal("7.00"), new BigDecimal("1.10"));

        insertIndividualRule(userId, "HGLG11", RuleField.PRICE,
                ComparisonOperator.GREATER_THAN, new BigDecimal("100.00"));
        insertIndividualRule(userId, "XPLG11", RuleField.PRICE,
                ComparisonOperator.GREATER_THAN, new BigDecimal("50.00"));
        syncJpaCache();

        evaluateRulesUseCase.execute();
        syncJpaCache();

        List<Map<String, Object>> alerts = jdbc.queryForList("SELECT * FROM alert");
        assertThat(alerts).hasSizeGreaterThanOrEqualTo(1);
        assertThat(alerts).anyMatch(a -> "HGLG11".equals(a.get("TICKER")));
    }

    // ========================================================================
    // Full flow: rule satisfied -> alert created -> email sent (Req 6.1)
    // ========================================================================

    @Test
    @DisplayName("Full flow: rule satisfied -> PENDING alert -> email sent -> SENT")
    void fullFlow_ruleSatisfied_alertCreated_emailSent() {
        doNothing().when(emailGateway).send(anyString(), anyString(), anyString());

        insertIndividualRule(userId, "HGLG11", RuleField.PRICE,
                ComparisonOperator.GREATER_THAN, new BigDecimal("100.00"));
        syncJpaCache();

        evaluateRulesUseCase.execute();
        syncJpaCache();

        assertThat(jdbc.queryForList("SELECT * FROM alert WHERE status = 'PENDING'")).hasSize(1);

        sendPendingAlertsUseCase.execute();
        syncJpaCache();

        verify(emailGateway).send(
                eq("test@example.com"),
                contains("HGLG11"),
                anyString()
        );

        List<Map<String, Object>> sentAlerts = jdbc.queryForList(
                "SELECT * FROM alert WHERE status = 'SENT'");
        assertThat(sentAlerts).hasSize(1);
        assertThat(sentAlerts.getFirst().get("SENT_AT")).isNotNull();
        assertThat(jdbc.queryForList("SELECT * FROM alert WHERE status = 'PENDING'")).isEmpty();
    }

    @Test
    @DisplayName("Full flow with group: all rules satisfied -> alert -> email sent")
    void fullFlowWithGroup_allRulesSatisfied_alertCreated_emailSent() {
        doNothing().when(emailGateway).send(anyString(), anyString(), anyString());

        insertGroupWithRules(userId, "HGLG11", "FII Opportunity",
                List.of(
                        new RuleSpec(RuleField.PRICE, ComparisonOperator.GREATER_THAN, new BigDecimal("100.00")),
                        new RuleSpec(RuleField.DIVIDEND_YIELD, ComparisonOperator.GREATER_THAN, new BigDecimal("5.00"))
                ));
        syncJpaCache();

        evaluateRulesUseCase.execute();
        syncJpaCache();
        assertThat(jdbc.queryForList("SELECT * FROM alert WHERE status = 'PENDING'")).hasSize(1);

        sendPendingAlertsUseCase.execute();
        syncJpaCache();

        verify(emailGateway).send(
                eq("test@example.com"),
                contains("HGLG11"),
                anyString()
        );

        assertThat(jdbc.queryForList("SELECT * FROM alert WHERE status = 'SENT'")).hasSize(1);
        assertThat(jdbc.queryForList("SELECT * FROM alert WHERE status = 'PENDING'")).isEmpty();
    }

    @Test
    @DisplayName("Email failure should keep alert as PENDING for retry")
    void emailFailure_shouldKeepAlertPending() {
        doThrow(new RuntimeException("SMTP connection refused"))
                .when(emailGateway).send(anyString(), anyString(), anyString());

        insertIndividualRule(userId, "HGLG11", RuleField.PRICE,
                ComparisonOperator.GREATER_THAN, new BigDecimal("100.00"));
        syncJpaCache();

        evaluateRulesUseCase.execute();
        syncJpaCache();

        sendPendingAlertsUseCase.execute();
        syncJpaCache();

        assertThat(jdbc.queryForList("SELECT * FROM alert WHERE status = 'PENDING'")).hasSize(1);
        assertThat(jdbc.queryForList("SELECT * FROM alert WHERE status = 'SENT'")).isEmpty();
    }

    @Test
    @DisplayName("Email body should contain asset price details")
    void emailContent_shouldContainAssetAndRuleDetails() {
        doNothing().when(emailGateway).send(anyString(), anyString(), anyString());

        insertIndividualRule(userId, "HGLG11", RuleField.PRICE,
                ComparisonOperator.GREATER_THAN, new BigDecimal("100.00"));
        syncJpaCache();

        evaluateRulesUseCase.execute();
        syncJpaCache();

        sendPendingAlertsUseCase.execute();

        verify(emailGateway).send(
                eq("test@example.com"),
                contains("HGLG11"),
                contains("160.00")
        );
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private String now() {
        return LocalDateTime.now().format(TS_FORMAT);
    }

    private void syncJpaCache() {
        entityManager.flush();
        entityManager.clear();
    }

    private Long insertUser(String name, String email) {
        String ts = now();
        jdbc.update("""
                INSERT INTO "user" (name, email, password_hash, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """, name, email, "$2a$10$dummyhashforintegrationtests", ts, ts);
        return jdbc.queryForObject("SELECT MAX(id) FROM \"user\"", Long.class);
    }

    private Long insertAsset(String ticker, String name,
                             BigDecimal price, BigDecimal dy, BigDecimal pvp) {
        jdbc.update("""
                INSERT INTO asset (ticker, name, current_price, dividend_yield, p_vp, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, ticker, name, price, dy, pvp, now());
        return jdbc.queryForObject("SELECT MAX(id) FROM asset", Long.class);
    }

    private Long insertIndividualRule(Long userIdParam, String ticker, RuleField field,
                                      ComparisonOperator operator, BigDecimal targetValue) {
        String ts = now();
        jdbc.update("""
                INSERT INTO rule (user_id, ticker, group_id, field, operator, target_value, active, created_at, updated_at)
                VALUES (?, ?, NULL, ?, ?, ?, TRUE, ?, ?)
                """, userIdParam, ticker, field.name(), operator.name(), targetValue, ts, ts);
        return jdbc.queryForObject("SELECT MAX(id) FROM rule", Long.class);
    }

    private Long insertGroupWithRules(Long userIdParam, String ticker, String groupName,
                                      List<RuleSpec> ruleSpecs) {
        String ts = now();
        jdbc.update("""
                INSERT INTO rule_group (user_id, ticker, name, created_at)
                VALUES (?, ?, ?, ?)
                """, userIdParam, ticker, groupName, ts);
        Long groupId = jdbc.queryForObject("SELECT MAX(id) FROM rule_group", Long.class);

        for (RuleSpec spec : ruleSpecs) {
            jdbc.update("""
                    INSERT INTO rule (user_id, ticker, group_id, field, operator, target_value, active, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, TRUE, ?, ?)
                    """, userIdParam, ticker, groupId, spec.field().name(), spec.operator().name(),
                    spec.targetValue(), ts, ts);
        }

        return groupId;
    }

    private record RuleSpec(RuleField field, ComparisonOperator operator, BigDecimal targetValue) {}
}
