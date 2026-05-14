package com.invest.infrastructure.config.security;

import com.invest.domain.entities.User;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Size;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property-based tests for JwtTokenProvider.
 * Validates: Requirements 4.1, 4.2, 4.4
 */
class JwtTokenProviderProperties {

    private static final String SECRET =
            "this-is-a-test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256";
    private static final long EXPIRATION_MS = 86400000L;

    private final JwtTokenProvider tokenProvider = new JwtTokenProvider(SECRET, EXPIRATION_MS);

    private User buildUser(long id) {
        return new User(id, "Test User", "test@example.com", "hash",
                LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * Property 6: JWT permissions claim round-trip.
     * For any non-empty collection of permission name strings, generating a JWT token
     * with those permissions and then extracting the permissions claim must return a list
     * containing exactly the same permission names (order-independent).
     * Validates: Requirements 4.1, 4.2
     */
    @Property(tries = 100)
    void permissionsClaimRoundTrip(
            @ForAll("nonEmptyPermissionLists") List<String> permissions) {

        User user = buildUser(1L);
        String token = tokenProvider.generateToken(user, permissions);

        assertNotNull(token, "Generated token must not be null");

        List<String> extracted = tokenProvider.extractPermissions(token);

        assertNotNull(extracted, "Extracted permissions must not be null");
        assertEquals(
                new HashSet<>(permissions),
                new HashSet<>(extracted),
                "Extracted permissions must match the original set (order-independent)"
        );
    }

    /**
     * Property 7: Missing permissions claim produces empty authorities.
     * For any JWT token generated with an empty permissions list, extracting permissions
     * must return an empty list without throwing an exception.
     * Validates: Requirements 4.4
     */
    @Property(tries = 100)
    void emptyPermissionsProducesEmptyList(
            @ForAll @NotBlank String name,
            @ForAll("validUserIds") Long userId) {

        User user = buildUser(userId);
        String token = tokenProvider.generateToken(user, List.of());

        List<String> extracted = tokenProvider.extractPermissions(token);

        assertNotNull(extracted, "Extracted permissions must not be null for empty claim");
        assertTrue(extracted.isEmpty(),
                "Extracted permissions must be empty when token was generated with empty list");
    }

    /**
     * Additional property: permissions count is preserved in round-trip.
     * Validates: Requirements 4.1
     */
    @Property(tries = 100)
    void permissionsCountIsPreservedInRoundTrip(
            @ForAll("nonEmptyPermissionLists") List<String> permissions) {

        User user = buildUser(1L);
        String token = tokenProvider.generateToken(user, permissions);
        List<String> extracted = tokenProvider.extractPermissions(token);

        Set<String> originalSet = new HashSet<>(permissions);
        Set<String> extractedSet = new HashSet<>(extracted);

        assertEquals(originalSet.size(), extractedSet.size(),
                "Number of unique permissions must be preserved in round-trip");
    }

    @Provide
    Arbitrary<List<String>> nonEmptyPermissionLists() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(30)
                .map(String::toUpperCase)
                .list()
                .ofMinSize(1)
                .ofMaxSize(10);
    }

    @Provide
    Arbitrary<Long> validUserIds() {
        return Arbitraries.longs().between(1L, Long.MAX_VALUE);
    }
}
