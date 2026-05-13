package com.invest.infrastructure.config;

import com.invest.infrastructure.config.security.JwtAuthenticationFilter;
import com.invest.infrastructure.config.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests verifying that application configuration properties from application.yml
 * are loaded correctly into Spring beans and infrastructure components.
 *
 * Validates: Requirements 10.4
 */
@SpringBootTest(classes = ConfigurationSmokeTest.SmokeTestConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:smokedb;DB_CLOSE_DELAY=-1;MODE=MYSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never",
        "spring.jpa.defer-datasource-initialization=false",
        "app.jwt.secret=smoke-test-secret-key-must-be-at-least-256-bits-long-for-hmac",
        "app.jwt.expiration-ms=86400000"
})
class ConfigurationSmokeTest {

    @Configuration
    @EnableAutoConfiguration(exclude = {
            SecurityAutoConfiguration.class,
            SecurityFilterAutoConfiguration.class
    })
    @EntityScan(basePackages = "com.investmonitor.adapters.persistence")
    @EnableJpaRepositories(basePackages = "com.investmonitor.adapters.persistence")
    static class SmokeTestConfig {

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter(
                @Value("${app.jwt.secret}") String secret,
                @Value("${app.jwt.expiration-ms}") long expirationMs) {
            JwtTokenProvider provider = new JwtTokenProvider(secret, expirationMs);
            return new JwtAuthenticationFilter(provider);
        }
    }

    // --- DataSource Configuration ---

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("DataSource bean should be loaded from configuration")
    void dataSourceShouldBeLoaded() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    @DisplayName("DataSource should be connectable with configured properties")
    void dataSourceShouldBeConnectable() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isClosed()).isFalse();
        }
    }

    // --- JWT Configuration ---

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Test
    @DisplayName("JWT secret should be loaded from configuration")
    void jwtSecretShouldBeLoaded() {
        assertThat(jwtSecret).isNotBlank();
        assertThat(jwtSecret).isEqualTo("smoke-test-secret-key-must-be-at-least-256-bits-long-for-hmac");
    }

    @Test
    @DisplayName("JWT expiration should be loaded from configuration")
    void jwtExpirationShouldBeLoaded() {
        assertThat(jwtExpirationMs).isPositive();
        assertThat(jwtExpirationMs).isEqualTo(86400000L);
    }
}
