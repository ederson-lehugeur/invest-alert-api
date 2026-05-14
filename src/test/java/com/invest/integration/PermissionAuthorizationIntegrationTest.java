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
 * Integration tests that verify permission-based authorization across all protected endpoints.
 *
 * These tests cover three categories:
 *
 * 1. Endpoints WITH @PreAuthorize - verify that missing the required permission returns 403.
 * 2. RuleGroupController#create - now protected with @PreAuthorize("hasAuthority('ALERT_CREATE')").
 * 3. JWT stale-permissions - documented as a known limitation: permissions in the token are
 *    not re-validated against the DB on each request. Mitigated by short access token TTL (15 min)
 *    combined with refresh token rotation.
 * 4. AuthenticationEntryPoint - unauthenticated requests now correctly return 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Permission-based authorization integration tests")
class PermissionAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    // -------------------------------------------------------------------------
    // Token factory helpers
    // -------------------------------------------------------------------------

    private String tokenWith(List<String> permissions) {
        User user = new User(99L, "Test User", "test@example.com", "hash",
                LocalDateTime.now(), LocalDateTime.now());
        return tokenProvider.generateToken(user, permissions);
    }

    private String tokenWithAllAlertPermissions() {
        return tokenWith(List.of("ALERT_CREATE", "ALERT_UPDATE", "ALERT_DELETE"));
    }

    private String tokenWithNoPermissions() {
        return tokenWith(Collections.emptyList());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/rules  -  requires ALERT_CREATE
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/rules - ALERT_CREATE enforcement")
    class CreateRulePermission {

        private static final String RULE_BODY = """
                {"ticker":"XPLG11","field":"PRICE","operator":"GREATER_THAN","targetValue":100}
                """;

        @Test
        @DisplayName("Returns 403 when token carries no permissions")
        void forbiddenWithNoPermissions() throws Exception {
            mockMvc.perform(post("/api/v1/rules")
                            .header("Authorization", "Bearer " + tokenWithNoPermissions())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(RULE_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 403 when token carries ALERT_UPDATE and ALERT_DELETE but not ALERT_CREATE")
        void forbiddenWithUnrelatedPermissions() throws Exception {
            String token = tokenWith(List.of("ALERT_UPDATE", "ALERT_DELETE"));

            mockMvc.perform(post("/api/v1/rules")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(RULE_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 401 when no token is provided")
        void returns403WithNoToken_shouldBe401() throws Exception {
            mockMvc.perform(post("/api/v1/rules")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(RULE_BODY))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Proceeds past authorization (not 403) when token carries ALERT_CREATE")
        void notForbiddenWithAlertCreate() throws Exception {
            String token = tokenWith(List.of("ALERT_CREATE"));

            // The request will fail for business reasons (asset not found, etc.) but must NOT be 403.
            int status = mockMvc.perform(post("/api/v1/rules")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(RULE_BODY))
                    .andReturn().getResponse().getStatus();

            assert status != 403 : "Expected authorization to pass with ALERT_CREATE, but got 403";
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/rules/{id}  -  requires ALERT_UPDATE
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/v1/rules/{id} - ALERT_UPDATE enforcement")
    class UpdateRulePermission {

        private static final String UPDATE_BODY = """
                {"field":"PRICE","operator":"GREATER_THAN","targetValue":150}
                """;

        @Test
        @DisplayName("Returns 403 when token carries no permissions")
        void forbiddenWithNoPermissions() throws Exception {
            mockMvc.perform(put("/api/v1/rules/1")
                            .header("Authorization", "Bearer " + tokenWithNoPermissions())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 403 when token carries ALERT_CREATE and ALERT_DELETE but not ALERT_UPDATE")
        void forbiddenWithUnrelatedPermissions() throws Exception {
            String token = tokenWith(List.of("ALERT_CREATE", "ALERT_DELETE"));

            mockMvc.perform(put("/api/v1/rules/1")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 401 when no token is provided")
        void returns403WithNoToken_shouldBe401() throws Exception {
            mockMvc.perform(put("/api/v1/rules/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_BODY))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Proceeds past authorization (not 403) when token carries ALERT_UPDATE")
        void notForbiddenWithAlertUpdate() throws Exception {
            String token = tokenWith(List.of("ALERT_UPDATE"));

            int status = mockMvc.perform(put("/api/v1/rules/1")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_BODY))
                    .andReturn().getResponse().getStatus();

            assert status != 403 : "Expected authorization to pass with ALERT_UPDATE, but got 403";
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/rules/{id}  -  requires ALERT_DELETE
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/v1/rules/{id} - ALERT_DELETE enforcement")
    class DeleteRulePermission {

        @Test
        @DisplayName("Returns 403 when token carries no permissions")
        void forbiddenWithNoPermissions() throws Exception {
            mockMvc.perform(delete("/api/v1/rules/1")
                            .header("Authorization", "Bearer " + tokenWithNoPermissions()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 403 when token carries ALERT_CREATE and ALERT_UPDATE but not ALERT_DELETE")
        void forbiddenWithUnrelatedPermissions() throws Exception {
            String token = tokenWith(List.of("ALERT_CREATE", "ALERT_UPDATE"));

            mockMvc.perform(delete("/api/v1/rules/1")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 401 when no token is provided")
        void returns403WithNoToken_shouldBe401() throws Exception {
            mockMvc.perform(delete("/api/v1/rules/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Proceeds past authorization (not 403) when token carries ALERT_DELETE")
        void notForbiddenWithAlertDelete() throws Exception {
            String token = tokenWith(List.of("ALERT_DELETE"));

            int status = mockMvc.perform(delete("/api/v1/rules/1")
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getStatus();

            assert status != 403 : "Expected authorization to pass with ALERT_DELETE, but got 403";
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/rules  -  no @PreAuthorize, only requires authentication
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/rules - authentication only (no permission required)")
    class ListRulesPermission {

        @Test
        @DisplayName("Returns 401 when no token is provided")
        void returns403WithNoToken_shouldBe401() throws Exception {
            mockMvc.perform(get("/api/v1/rules"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Returns 200 when token carries no permissions (only authentication required)")
        void okWithNoPermissions() throws Exception {
            mockMvc.perform(get("/api/v1/rules")
                            .header("Authorization", "Bearer " + tokenWithNoPermissions()))
                    .andExpect(status().isOk());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/alerts  -  no @PreAuthorize, only requires authentication
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/alerts - authentication only (no permission required)")
    class ListAlertsPermission {

        @Test
        @DisplayName("Returns 401 when no token is provided")
        void returns403WithNoToken_shouldBe401() throws Exception {
            mockMvc.perform(get("/api/v1/alerts"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Returns 200 when token carries no permissions (only authentication required)")
        void okWithNoPermissions() throws Exception {
            mockMvc.perform(get("/api/v1/alerts")
                            .header("Authorization", "Bearer " + tokenWithNoPermissions()))
                    .andExpect(status().isOk());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/rule-groups  -  BUG: missing @PreAuthorize
    // This endpoint creates rule groups but has no permission guard.
    // Any authenticated user - even one with no permissions - can call it.
    // The tests below document the current (broken) behavior and the expected behavior.
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/rule-groups - MISSING @PreAuthorize (bug)")
    class CreateRuleGroupPermission {

        private static final String GROUP_BODY = """
                {"ticker":"XPLG11","name":"My Group","rules":[]}
                """;

        @Test
        @DisplayName("Returns 401 when no token is provided")
        void returns403WithNoToken_shouldBe401() throws Exception {
            mockMvc.perform(post("/api/v1/rule-groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(GROUP_BODY))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * After adding @PreAuthorize("hasAuthority('ALERT_CREATE')") to RuleGroupController#create,
         * a token with no permissions must now be rejected with 403.
         */
        @Test
        @DisplayName("Returns 403 when token carries no permissions (bug fixed)")
        void forbiddenWithNoPermissions() throws Exception {
            mockMvc.perform(post("/api/v1/rule-groups")
                            .header("Authorization", "Bearer " + tokenWithNoPermissions())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(GROUP_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Proceeds past authorization (not 403) when token carries ALERT_CREATE")
        void notForbiddenWithAlertCreate() throws Exception {
            String token = tokenWith(List.of("ALERT_CREATE"));

            int status = mockMvc.perform(post("/api/v1/rule-groups")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(GROUP_BODY))
                    .andReturn().getResponse().getStatus();

            assert status != 403
                    : "Expected authorization to pass with ALERT_CREATE, but got 403";
        }
    }

    // -------------------------------------------------------------------------
    // JWT stale-permissions - known limitation, mitigated by short TTL
    //
    // Permissions are embedded in the JWT at login time. If a user's permissions
    // are revoked in the database, their existing access token still carries the
    // old permissions until it expires (default: 15 minutes).
    //
    // Mitigation: short access token TTL (15 min) + refresh token rotation.
    // When the access token expires, the refresh endpoint re-reads permissions
    // from the DB and issues a new token with the current permission set.
    // Revoking the refresh token (logout) prevents new access tokens from being issued.
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("JWT stale-permissions - mitigated by short TTL + refresh rotation")
    class StalePermissionsVulnerability {

        @Test
        @DisplayName("Stale access token still authorizes within its TTL window (expected behavior)")
        void staleTokenStillAuthorizesWithinTtl() throws Exception {
            // A token minted with ALERT_CREATE continues to work until it expires (15 min).
            // This is expected and acceptable given the short TTL.
            String tokenMintedBeforeRevocation = tokenWith(List.of("ALERT_CREATE"));

            String body = """
                    {"ticker":"XPLG11","field":"PRICE","operator":"GREATER_THAN","targetValue":100}
                    """;

            int status = mockMvc.perform(post("/api/v1/rules")
                            .header("Authorization", "Bearer " + tokenMintedBeforeRevocation)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn().getResponse().getStatus();

            // Not 403 - the token is still valid within its TTL.
            // After expiry, the refresh endpoint will re-read permissions from the DB.
            assert status != 403 : "Token within TTL should not be rejected with 403";
        }
    }
}
