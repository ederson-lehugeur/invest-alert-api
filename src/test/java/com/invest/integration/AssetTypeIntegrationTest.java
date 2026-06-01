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
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for multi-asset-type-support feature.
 * Validates: Requirements 8.1, 8.2, 8.4, 8.5, 4.1, 2.5
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Asset type and indicator integration tests")
class AssetTypeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    private String tokenWithAlertCreate() {
        User user = new User(1L, "Test User", "test@example.com", "hash",
                LocalDateTime.now(), LocalDateTime.now());
        return tokenProvider.generateToken(user, List.of("ALERT_CREATE"));
    }

    private String tokenWithNoPermissions() {
        User user = new User(1L, "Test User", "test@example.com", "hash",
                LocalDateTime.now(), LocalDateTime.now());
        return tokenProvider.generateToken(user, List.of());
    }

    @Nested
    @DisplayName("GET /api/v1/assets/{ticker}")
    class GetAssetEndpoint {

        @Test
        @DisplayName("Returns assetType and indicators for a known FII asset")
        void returnsAssetTypeAndIndicators() throws Exception {
            mockMvc.perform(get("/api/v1/assets/XPLG11")
                            .header("Authorization", "Bearer " + tokenWithNoPermissions()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticker", is("XPLG11")))
                    .andExpect(jsonPath("$.assetType", is("FII")))
                    .andExpect(jsonPath("$.indicators", notNullValue()))
                    .andExpect(jsonPath("$.indicators", hasSize(3)))
                    .andExpect(jsonPath("$.indicators[*].code",
                            containsInAnyOrder("PRICE", "DIVIDEND_YIELD", "PVP")));
        }

        @Test
        @DisplayName("Returns 404 for unknown ticker")
        void returns404ForUnknownTicker() throws Exception {
            mockMvc.perform(get("/api/v1/assets/UNKNOWN99")
                            .header("Authorization", "Bearer " + tokenWithNoPermissions()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/asset-types/{assetType}/indicators")
    class GetSupportedIndicatorsEndpoint {

        @Test
        @DisplayName("Returns correct indicators for FII")
        void returnsFiiIndicators() throws Exception {
            mockMvc.perform(get("/api/v1/asset-types/FII/indicators")
                            .header("Authorization", "Bearer " + tokenWithNoPermissions()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$", containsInAnyOrder("PRICE", "DIVIDEND_YIELD", "PVP")));
        }

        @Test
        @DisplayName("Returns only PRICE for CRYPTOCURRENCY")
        void returnsCryptocurrencyIndicators() throws Exception {
            mockMvc.perform(get("/api/v1/asset-types/CRYPTOCURRENCY/indicators")
                            .header("Authorization", "Bearer " + tokenWithNoPermissions()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$", hasItem("PRICE")));
        }

        @Test
        @DisplayName("Returns correct indicators for STOCK")
        void returnsStockIndicators() throws Exception {
            mockMvc.perform(get("/api/v1/asset-types/STOCK/indicators")
                            .header("Authorization", "Bearer " + tokenWithNoPermissions()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)))
                    .andExpect(jsonPath("$",
                            containsInAnyOrder("PRICE", "DIVIDEND_YIELD", "PVP", "PL", "ROE")));
        }

        @Test
        @DisplayName("Returns 400 for invalid asset type")
        void returns400ForInvalidAssetType() throws Exception {
            mockMvc.perform(get("/api/v1/asset-types/INVALID_TYPE/indicators")
                            .header("Authorization", "Bearer " + tokenWithNoPermissions()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/rules - indicator validation")
    class CreateRuleIndicatorValidation {

        @Test
        @DisplayName("Returns 422 when indicator is not supported for the asset type")
        void returns422ForIncompatibleIndicator() throws Exception {
            // XPLG11 is a FII - PL is not supported for FII
            String body = """
                    {
                        "ticker": "XPLG11",
                        "indicatorCode": "PL",
                        "operator": "GREATER_THAN",
                        "targetValue": 10
                    }
                    """;

            mockMvc.perform(post("/api/v1/rules")
                            .header("Authorization", "Bearer " + tokenWithAlertCreate())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Returns 400 when indicator code is unknown")
        void returns400ForUnknownIndicatorCode() throws Exception {
            String body = """
                    {
                        "ticker": "XPLG11",
                        "indicatorCode": "TOTALLY_UNKNOWN",
                        "operator": "GREATER_THAN",
                        "targetValue": 10
                    }
                    """;

            mockMvc.perform(post("/api/v1/rules")
                            .header("Authorization", "Bearer " + tokenWithAlertCreate())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 201 when indicator is compatible with asset type")
        void returns201ForCompatibleIndicator() throws Exception {
            String body = """
                    {
                        "ticker": "XPLG11",
                        "indicatorCode": "PRICE",
                        "operator": "LESS_THAN",
                        "targetValue": 200
                    }
                    """;

            mockMvc.perform(post("/api/v1/rules")
                            .header("Authorization", "Bearer " + tokenWithAlertCreate())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.indicatorType", is("PRICE")));
        }
    }
}
