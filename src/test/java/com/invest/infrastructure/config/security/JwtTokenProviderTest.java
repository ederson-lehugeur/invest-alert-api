package com.invest.infrastructure.config.security;

import com.invest.domain.entities.User;
import com.invest.domain.exceptions.ExpiredTokenException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String SECRET = "this-is-a-test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256";
    private static final long EXPIRATION_MS = 86400000L; // 24 hours

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, EXPIRATION_MS);
    }

    @Test
    void shouldGenerateValidToken() {
        User user = new User(42L, "John", "john@example.com", "hash",
                LocalDateTime.now(), LocalDateTime.now());

        String token = tokenProvider.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void shouldExtractUserIdFromToken() {
        User user = new User(99L, "Jane", "jane@example.com", "hash",
                LocalDateTime.now(), LocalDateTime.now());

        String token = tokenProvider.generateToken(user);
        Long extractedId = tokenProvider.extractUserId(token);

        assertEquals(99L, extractedId);
    }

    @Test
    void shouldValidateValidToken() {
        User user = new User(1L, "Test", "test@example.com", "hash",
                LocalDateTime.now(), LocalDateTime.now());

        String token = tokenProvider.generateToken(user);

        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void shouldReturnFalseForInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    void shouldReturnFalseForNullToken() {
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    void shouldReturnFalseForEmptyToken() {
        assertFalse(tokenProvider.validateToken(""));
    }

    @Test
    void shouldReturnFalseForExpiredToken() {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(SECRET, 0L);
        User user = new User(1L, "Test", "test@example.com", "hash",
                LocalDateTime.now(), LocalDateTime.now());

        String token = shortLivedProvider.generateToken(user);

        assertFalse(shortLivedProvider.validateToken(token));
    }

    @Test
    void shouldThrowExpiredTokenExceptionWhenExtractingFromExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("1")
                .issuedAt(new Date(System.currentTimeMillis() - 20000))
                .expiration(new Date(System.currentTimeMillis() - 10000))
                .signWith(key)
                .compact();

        assertThrows(ExpiredTokenException.class,
                () -> tokenProvider.extractUserId(expiredToken));
    }

    @Test
    void shouldReturnFalseForTokenSignedWithDifferentKey() {
        SecretKey differentKey = Keys.hmacShaKeyFor(
                "a-completely-different-secret-key-that-is-also-256-bits-long-for-testing".getBytes(StandardCharsets.UTF_8));
        String tokenWithDifferentKey = Jwts.builder()
                .subject("1")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(differentKey)
                .compact();

        assertFalse(tokenProvider.validateToken(tokenWithDifferentKey));
    }

    @Test
    void shouldReturnExpirationInSeconds() {
        assertEquals(86400L, tokenProvider.getExpirationInSeconds());
    }
}
