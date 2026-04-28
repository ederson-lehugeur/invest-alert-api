package com.invest.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration tests verifying deprecation and sunset headers on versioned endpoints.
 * Uses @TestPropertySource to override v1 as deprecated with a sunset date.
 *
 * Validates: Requirements 10.3, 7.1, 7.2, 7.4
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.api.versions.v1.deprecated=true",
        "app.api.versions.v1.sunset-date=2026-06-01"
})
class DeprecationHeaderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Deprecated v1 endpoints should include deprecation headers")
    class DeprecatedVersionHeaders {

        @Test
        @DisplayName("POST /api/v1/auth/login includes Deprecation header when v1 is deprecated")
        void deprecatedVersionIncludesDeprecationHeader() throws Exception {
            String body = """
                    {"email": "test@example.com", "password": "secret123"}
                    """;

            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn();

            assertThat(result.getResponse().getHeader("Deprecation")).isEqualTo("true");
        }

        @Test
        @DisplayName("POST /api/v1/auth/login includes Sunset header when v1 is deprecated with sunset date")
        void deprecatedVersionIncludesSunsetHeader() throws Exception {
            String body = """
                    {"email": "test@example.com", "password": "secret123"}
                    """;

            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn();

            String sunsetHeader = result.getResponse().getHeader("Sunset");
            assertThat(sunsetHeader).isNotNull();
            assertThat(sunsetHeader).isEqualTo("Mon, 1 Jun 2026 00:00:00 GMT");
        }

        @Test
        @DisplayName("POST /api/v1/auth/login includes both Deprecation and Sunset headers together")
        void deprecatedVersionIncludesBothHeaders() throws Exception {
            String body = """
                    {"email": "test@example.com", "password": "secret123"}
                    """;

            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn();

            assertThat(result.getResponse().getHeader("Deprecation")).isEqualTo("true");
            assertThat(result.getResponse().getHeader("Sunset")).isNotNull();
        }
    }
}
