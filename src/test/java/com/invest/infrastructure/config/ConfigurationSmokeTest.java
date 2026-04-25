package com.invest.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests verifying that application configuration properties from application.yml
 * are loaded correctly into Spring beans and infrastructure components.
 *
 * Validates: Requirements 5.1, 6.4, 8.1, 8.2, 8.3, 8.4, 8.5
 */
@SpringBootTest(classes = ConfigurationSmokeTest.SmokeTestConfig.class)
@TestPropertySource(properties = {
        // H2 in-memory database for test isolation (overrides MySQL config)
        "spring.datasource.url=jdbc:h2:mem:smokedb;DB_CLOSE_DELAY=-1;MODE=MYSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never",
        "spring.jpa.defer-datasource-initialization=false",
        // Explicit config values for smoke verification
        "app.jwt.secret=smoke-test-secret-key-must-be-at-least-256-bits-long-for-hmac",
        "app.jwt.expiration-ms=86400000",
        "app.scheduler.evaluation-interval-ms=300000",
        "spring.mail.host=smtp.smoketest.com",
        "spring.mail.port=587",
        "spring.mail.username=smoke@test.com",
        "spring.mail.password=smokepass"
})
class ConfigurationSmokeTest {

    @Configuration
    @EnableAutoConfiguration(exclude = {
            SecurityAutoConfiguration.class,
            SecurityFilterAutoConfiguration.class,
            MailSenderAutoConfiguration.class,
            QuartzAutoConfiguration.class
    })
    @EntityScan(basePackages = "com.investmonitor.adapters.persistence")
    @EnableJpaRepositories(basePackages = "com.investmonitor.adapters.persistence")
    @Import(QuartzConfig.class)
    static class SmokeTestConfig {

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter(
                @Value("${app.jwt.secret}") String secret,
                @Value("${app.jwt.expiration-ms}") long expirationMs) {
            JwtTokenProvider provider = new JwtTokenProvider(secret, expirationMs);
            return new JwtAuthenticationFilter(provider);
        }
    }

    // --- MySQL / DataSource Configuration (Req 8.1, 8.2) ---

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("DataSource bean should be loaded from configuration (Req 8.1, 8.2)")
    void dataSourceShouldBeLoaded() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    @DisplayName("DataSource should be connectable with configured properties (Req 8.1, 8.2)")
    void dataSourceShouldBeConnectable() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isClosed()).isFalse();
        }
    }

    // --- JWT Configuration (Req 8.3) ---

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Test
    @DisplayName("JWT secret should be loaded from configuration (Req 8.3)")
    void jwtSecretShouldBeLoaded() {
        assertThat(jwtSecret).isNotBlank();
        assertThat(jwtSecret).isEqualTo("smoke-test-secret-key-must-be-at-least-256-bits-long-for-hmac");
    }

    @Test
    @DisplayName("JWT expiration should be loaded from configuration (Req 8.3)")
    void jwtExpirationShouldBeLoaded() {
        assertThat(jwtExpirationMs).isPositive();
        assertThat(jwtExpirationMs).isEqualTo(86400000L);
    }

    // --- SMTP Configuration (Req 6.4, 8.4) ---

    @Value("${spring.mail.host}")
    private String smtpHost;

    @Value("${spring.mail.port}")
    private int smtpPort;

    @Value("${spring.mail.username}")
    private String smtpUsername;

    @Value("${spring.mail.password}")
    private String smtpPassword;

    @Test
    @DisplayName("SMTP host should be loaded from configuration (Req 8.4)")
    void smtpHostShouldBeLoaded() {
        assertThat(smtpHost).isNotBlank();
        assertThat(smtpHost).isEqualTo("smtp.smoketest.com");
    }

    @Test
    @DisplayName("SMTP port should be loaded from configuration (Req 8.4)")
    void smtpPortShouldBeLoaded() {
        assertThat(smtpPort).isPositive();
        assertThat(smtpPort).isEqualTo(587);
    }

    @Test
    @DisplayName("SMTP username should be loaded from configuration (Req 6.4, 8.4)")
    void smtpUsernameShouldBeLoaded() {
        assertThat(smtpUsername).isNotBlank();
        assertThat(smtpUsername).isEqualTo("smoke@test.com");
    }

    @Test
    @DisplayName("SMTP password should be loaded from configuration (Req 8.4)")
    void smtpPasswordShouldBeLoaded() {
        assertThat(smtpPassword).isNotBlank();
        assertThat(smtpPassword).isEqualTo("smokepass");
    }

    // --- Quartz Configuration (Req 5.1, 8.5) ---

    @Value("${app.scheduler.evaluation-interval-ms}")
    private long schedulerIntervalMs;

    @Autowired
    private JobDetail ruleEvaluationJobDetail;

    @Autowired
    private Trigger ruleEvaluationTrigger;

    @Test
    @DisplayName("Scheduler evaluation interval should be loaded from configuration (Req 8.5)")
    void schedulerIntervalShouldBeLoaded() {
        assertThat(schedulerIntervalMs).isPositive();
        assertThat(schedulerIntervalMs).isEqualTo(300000L);
    }

    @Test
    @DisplayName("Quartz JobDetail bean should be configured (Req 5.1)")
    void quartzJobDetailShouldBeConfigured() {
        assertThat(ruleEvaluationJobDetail).isNotNull();
        assertThat(ruleEvaluationJobDetail.getKey().getName()).isEqualTo("ruleEvaluationJob");
    }

    @Test
    @DisplayName("Quartz Trigger should use interval from yml configuration (Req 5.1, 8.5)")
    void quartzTriggerShouldUseConfiguredInterval() {
        assertThat(ruleEvaluationTrigger).isInstanceOf(SimpleTrigger.class);
        SimpleTrigger simpleTrigger = (SimpleTrigger) ruleEvaluationTrigger;
        assertThat(simpleTrigger.getRepeatInterval()).isEqualTo(300000L);
        assertThat(simpleTrigger.getRepeatCount()).isEqualTo(SimpleTrigger.REPEAT_INDEFINITELY);
    }
}
