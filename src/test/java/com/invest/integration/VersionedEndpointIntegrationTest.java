package com.invest.integration;

import com.invest.domain.entities.User;
import com.invest.domain.ports.out.TokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying v1 endpoint accessibility and security rules.
 *
 * Validates: Requirements 10.1, 10.4, 8.1, 8.2
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VersionedEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    private String generateValidToken() {
        User user = new User(1L, "Test User", "test@example.com", "hash",
                LocalDateTime.now(), LocalDateTime.now());
        return tokenProvider.generateToken(user, Collections.emptyList());
    }

    @Nested
    @DisplayName("Public auth endpoints should be accessible without authentication")
    class PublicAuthEndpoints {

        @Test
        @DisplayName("POST /api/v1/auth/register is accessible without authentication")
        void registerEndpointIsAccessible() throws Exception {
            String body = """
                    {"name": "Test User", "email": "test@example.com", "password": "secret123"}
                    """;

            int status = mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            assertThat(status).isNotIn(401, 403);
        }

        @Test
        @DisplayName("POST /api/v1/auth/login is accessible without authentication")
        void loginEndpointIsAccessible() throws Exception {
            String body = """
                    {"email": "test@example.com", "password": "secret123"}
                    """;

            int status = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            assertThat(status).isNotIn(401, 403);
        }
    }

    @Nested
    @DisplayName("Protected endpoints should require authentication")
    class ProtectedEndpoints {

        @Test
        @DisplayName("GET /api/v1/assets is denied without authentication")
        void assetsRequiresAuthentication() throws Exception {
            int status = mockMvc.perform(get("/api/v1/assets"))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            assertThat(status).isIn(401, 403);
        }

        @Test
        @DisplayName("GET /api/v1/alerts is denied without authentication")
        void alertsRequiresAuthentication() throws Exception {
            int status = mockMvc.perform(get("/api/v1/alerts"))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            assertThat(status).isIn(401, 403);
        }

        @Test
        @DisplayName("GET /api/v1/rules is denied without authentication")
        void rulesRequiresAuthentication() throws Exception {
            int status = mockMvc.perform(get("/api/v1/rules"))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            assertThat(status).isIn(401, 403);
        }

        @Test
        @DisplayName("GET /api/v1/rule-groups is denied without authentication")
        void ruleGroupsRequiresAuthentication() throws Exception {
            int status = mockMvc.perform(get("/api/v1/rule-groups"))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            assertThat(status).isIn(401, 403);
        }
    }

    @Nested
    @DisplayName("Non-existent version paths should return 404")
    class NonExistentVersionPaths {

        @Test
        @DisplayName("GET /api/v99/assets returns 404 when authenticated")
        void nonExistentVersionReturns404() throws Exception {
            String token = generateValidToken();

            mockMvc.perform(get("/api/v99/assets")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Non-deprecated version should not include deprecation headers")
    class NonDeprecatedVersionHeaders {

        @Test
        @DisplayName("POST /api/v1/auth/login does not include Deprecation header when v1 is not deprecated")
        void nonDeprecatedVersionExcludesDeprecationHeader() throws Exception {
            String body = """
                    {"email": "test@example.com", "password": "secret123"}
                    """;

            var result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn();

            assertThat(result.getResponse().getHeader("Deprecation")).isNull();
            assertThat(result.getResponse().getHeader("Sunset")).isNull();
        }
    }

    /**
     * Validates: Requirements 6.1, 6.3
     */
    @Nested
    @DisplayName("OpenAPI versioned groups should expose correct specs")
    class OpenApiVersionedGroups {

        @Test
        @DisplayName("GET /v3/api-docs/v1 returns a valid OpenAPI spec containing v1 paths")
        void v1OpenApiSpecContainsV1Paths() throws Exception {
            mockMvc.perform(get("/v3/api-docs/v1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.paths['/api/v1/assets']").exists())
                    .andExpect(jsonPath("$.paths['/api/v1/alerts']").exists());
        }
    }
}
