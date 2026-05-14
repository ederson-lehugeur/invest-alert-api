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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying Spring Security method-level authorization.
 * Property 8: Unauthorized requests to protected endpoints return 403.
 * Validates: Requirements 5.2, 5.3, 5.4, 5.6, 11.4, 12.6
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    private String tokenWithPermissions(List<String> permissions) {
        User user = new User(1L, "Test User", "test@example.com", "hash",
                LocalDateTime.now(), LocalDateTime.now());
        return tokenProvider.generateToken(user, permissions);
    }

    private String tokenWithNoPermissions() {
        return tokenWithPermissions(Collections.emptyList());
    }

    @Nested
    @DisplayName("POST /api/v1/rules requires ALERT_CREATE authority")
    class CreateRuleAuthorization {

        @Test
        @DisplayName("Returns 403 when token has no ALERT_CREATE authority")
        void returns403WhenMissingAlertCreateAuthority() throws Exception {
            String token = tokenWithNoPermissions();
            String body = """
                    {"ticker": "XPLG11", "field": "PRICE", "operator": "GREATER_THAN", "targetValue": 100}
                    """;

            mockMvc.perform(post("/api/v1/rules")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 403 when token has ALERT_UPDATE but not ALERT_CREATE")
        void returns403WhenHasWrongAuthority() throws Exception {
            String token = tokenWithPermissions(List.of("ALERT_UPDATE", "ALERT_DELETE"));
            String body = """
                    {"ticker": "XPLG11", "field": "PRICE", "operator": "GREATER_THAN", "targetValue": 100}
                    """;

            mockMvc.perform(post("/api/v1/rules")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/rules/{id} requires ALERT_UPDATE authority")
    class UpdateRuleAuthorization {

        @Test
        @DisplayName("Returns 403 when token has no ALERT_UPDATE authority")
        void returns403WhenMissingAlertUpdateAuthority() throws Exception {
            String token = tokenWithNoPermissions();
            String body = """
                    {"field": "PRICE", "operator": "GREATER_THAN", "targetValue": 100}
                    """;

            mockMvc.perform(put("/api/v1/rules/1")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 403 when token has ALERT_CREATE but not ALERT_UPDATE")
        void returns403WhenHasWrongAuthority() throws Exception {
            String token = tokenWithPermissions(List.of("ALERT_CREATE", "ALERT_DELETE"));
            String body = """
                    {"field": "PRICE", "operator": "GREATER_THAN", "targetValue": 100}
                    """;

            mockMvc.perform(put("/api/v1/rules/1")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/rules/{id} requires ALERT_DELETE authority")
    class DeleteRuleAuthorization {

        @Test
        @DisplayName("Returns 403 when token has no ALERT_DELETE authority")
        void returns403WhenMissingAlertDeleteAuthority() throws Exception {
            String token = tokenWithNoPermissions();

            mockMvc.perform(delete("/api/v1/rules/1")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 403 when token has ALERT_CREATE but not ALERT_DELETE")
        void returns403WhenHasWrongAuthority() throws Exception {
            String token = tokenWithPermissions(List.of("ALERT_CREATE", "ALERT_UPDATE"));

            mockMvc.perform(delete("/api/v1/rules/1")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Public auth endpoints are accessible without a token")
    class PublicEndpointsAccessibility {

        @Test
        @DisplayName("POST /api/v1/auth/register is accessible without authentication (Requirement 11.4)")
        void registerIsAccessibleWithoutToken() throws Exception {
            String body = """
                    {"name": "Test User", "email": "newuser@example.com", "password": "secret123"}
                    """;

            int status = mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            // Must not be 401 or 403 - the endpoint is public
            assert status != 401 && status != 403
                    : "Register endpoint must be accessible without authentication, got: " + status;
        }

        @Test
        @DisplayName("POST /api/v1/auth/login is accessible without authentication (Requirement 11.4)")
        void loginIsAccessibleWithoutToken() throws Exception {
            String body = """
                    {"email": "nonexistent@example.com", "password": "wrongpass"}
                    """;

            int status = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            // Must not be 401 or 403 - the endpoint is public (may return 401 for wrong credentials
            // but that's a business-level 401, not a security filter 401)
            assert status != 403 : "Login endpoint must not return 403, got: " + status;
        }
    }
}
