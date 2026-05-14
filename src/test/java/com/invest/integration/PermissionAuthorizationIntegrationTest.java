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
 * 2. Endpoints WITHOUT @PreAuthorize - expose the gap where any authenticated user can access
 *    endpoints that should require a specific permission (e.g. RuleGroupController#create).
 * 3. JWT stale-permissions vulnerability - permissions embedded in the token at login time
 *    are never re-validated against the database on subsequent requests.
 *
 * Known secondary issue: SecurityConfig does not configure an AuthenticationEntryPoint,
 * so unauthenticated requests (no token) return 403 instead of the correct 401.
 * The "no token" tests below assert the actual behavior (403) and document the expected (401).
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
        @DisplayName("Returns 403 when no token is provided (BUG: should be 401 - missing AuthenticationEntryPoint)")
        void returns403WithNoToken_shouldBe401() throws Exception {
            // BUG: SecurityConfig has no AuthenticationEntryPoint configured.
            // Spring Security defaults to 403 for unauthenticated requests instead of 401.
            mockMvc.perform(post("/api/v1/rules")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(RULE_BODY))
                    .andExpect(status().isForbidden());
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
        @DisplayName("Returns 403 when no token is provided (BUG: should be 401 - missing AuthenticationEntryPoint)")
        void returns403WithNoToken_shouldBe401() throws Exception {
            mockMvc.perform(put("/api/v1/rules/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_BODY))
                    .andExpect(status().isForbidden());
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
        @DisplayName("Returns 403 when no token is provided (BUG: should be 401 - missing AuthenticationEntryPoint)")
        void returns403WithNoToken_shouldBe401() throws Exception {
            mockMvc.perform(delete("/api/v1/rules/1"))
                    .andExpect(status().isForbidden());
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
        @DisplayName("Returns 403 when no token is provided (BUG: should be 401 - missing AuthenticationEntryPoint)")
        void returns403WithNoToken_shouldBe401() throws Exception {
            mockMvc.perform(get("/api/v1/rules"))
                    .andExpect(status().isForbidden());
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
        @DisplayName("Returns 403 when no token is provided (BUG: should be 401 - missing AuthenticationEntryPoint)")
        void returns403WithNoToken_shouldBe401() throws Exception {
            mockMvc.perform(get("/api/v1/alerts"))
                    .andExpect(status().isForbidden());
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
        @DisplayName("Returns 403 when no token is provided (BUG: should be 401 - missing AuthenticationEntryPoint)")
        void returns403WithNoToken_shouldBe401() throws Exception {
            mockMvc.perform(post("/api/v1/rule-groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(GROUP_BODY))
                    .andExpect(status().isForbidden());
        }

        /**
         * BUG DEMONSTRATION: A token with NO permissions should be denied (403),
         * but because RuleGroupController#create has no @PreAuthorize annotation,
         * the request passes the authorization layer and reaches the use case.
         *
         * This test documents the current broken behavior (not 403).
         * Once the bug is fixed by adding @PreAuthorize("hasAuthority('ALERT_CREATE')")
         * to RuleGroupController#create, this test should be updated to expect 403.
         */
        @Test
        @DisplayName("BUG: token with no permissions is NOT rejected (should be 403 but is not)")
        void bugTokenWithNoPermissionsIsNotRejected() throws Exception {
            int status = mockMvc.perform(post("/api/v1/rule-groups")
                            .header("Authorization", "Bearer " + tokenWithNoPermissions())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(GROUP_BODY))
                    .andReturn().getResponse().getStatus();

            // Documents the bug: the response is NOT 403 even though it should be.
            assert status != 403
                    : "BUG FIXED: RuleGroupController#create now correctly returns 403 for missing permission. "
                    + "Update this test to expect 403.";
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
    // JWT stale-permissions vulnerability
    //
    // Permissions are embedded in the JWT at login time. If a user's permissions
    // are revoked in the database, their existing token still carries the old
    // permissions and continues to grant access until the token expires.
    //
    // This test documents the vulnerability: a token minted with ALERT_CREATE
    // continues to authorize POST /api/v1/rules even after the permission would
    // have been "revoked" - because the filter reads from the token, not the DB.
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("JWT stale-permissions vulnerability")
    class StalePermissionsVulnerability {

        @Test
        @DisplayName("Token minted with ALERT_CREATE still authorizes after simulated revocation")
        void staleTokenStillAuthorizes() throws Exception {
            // Simulate: user had ALERT_CREATE when they logged in.
            String tokenMintedBeforeRevocation = tokenWith(List.of("ALERT_CREATE"));

            // Simulate: permission was revoked in the DB (no action needed here because
            // the filter never consults the DB - this is the vulnerability).

            // The token still carries ALERT_CREATE and the filter accepts it.
            String body = """
                    {"ticker":"XPLG11","field":"PRICE","operator":"GREATER_THAN","targetValue":100}
                    """;

            int status = mockMvc.perform(post("/api/v1/rules")
                            .header("Authorization", "Bearer " + tokenMintedBeforeRevocation)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn().getResponse().getStatus();

            // The request is NOT rejected with 403 - the stale token still works.
            // This documents the vulnerability: revocation has no immediate effect.
            assert status != 403
                    : "Stale token was unexpectedly rejected. "
                    + "If this is intentional, the filter now validates permissions against the DB.";
        }
    }
}
