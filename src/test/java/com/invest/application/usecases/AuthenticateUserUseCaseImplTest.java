package com.invest.application.usecases;

import com.invest.application.commands.AuthenticateUserCommand;
import com.invest.application.responses.TokenResponse;
import com.invest.domain.entities.RefreshToken;
import com.invest.domain.entities.User;
import com.invest.domain.exceptions.InvalidCredentialsException;
import com.invest.domain.ports.out.PasswordEncoder;
import com.invest.domain.ports.out.RefreshTokenGenerator;
import com.invest.domain.ports.out.TokenProvider;
import com.invest.domain.ports.out.repositories.RefreshTokenRepository;
import com.invest.domain.ports.out.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    private static final long REFRESH_EXPIRATION_SECONDS = 604800L;

    private AuthenticateUserUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticateUserUseCaseImpl(
                userRepository, passwordEncoder, tokenProvider,
                refreshTokenRepository, refreshTokenGenerator, REFRESH_EXPIRATION_SECONDS);
    }

    @Test
    void shouldReturnAccessAndRefreshTokens_whenCredentialsAreValid() {
        var command = new AuthenticateUserCommand("john@example.com", "secret123");
        var user = new User(1L, "John", "john@example.com", "hashed", null, null);
        var savedRefreshToken = new RefreshToken(1L, 1L, "refresh-opaque-token",
                LocalDateTime.now().plusSeconds(REFRESH_EXPIRATION_SECONDS), false);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(tokenProvider.generateToken(eq(user), anyCollection())).thenReturn("jwt.access.token");
        when(tokenProvider.getExpirationInSeconds()).thenReturn(900L);
        when(refreshTokenGenerator.generate()).thenReturn("refresh-opaque-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedRefreshToken);

        TokenResponse response = useCase.execute(command);

        assertEquals("jwt.access.token", response.accessToken());
        assertEquals("refresh-opaque-token", response.refreshToken());
        assertEquals(900L, response.accessTokenExpiresIn());
        assertEquals(REFRESH_EXPIRATION_SECONDS, response.refreshTokenExpiresIn());
    }

    @Test
    void shouldRevokeExistingRefreshTokens_onLogin() {
        var command = new AuthenticateUserCommand("john@example.com", "secret123");
        var user = new User(1L, "John", "john@example.com", "hashed", null, null);
        var savedRefreshToken = new RefreshToken(1L, 1L, "new-token",
                LocalDateTime.now().plusSeconds(REFRESH_EXPIRATION_SECONDS), false);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(tokenProvider.generateToken(eq(user), anyCollection())).thenReturn("jwt.access.token");
        when(tokenProvider.getExpirationInSeconds()).thenReturn(900L);
        when(refreshTokenGenerator.generate()).thenReturn("new-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedRefreshToken);

        useCase.execute(command);

        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }

    @Test
    void shouldThrowInvalidCredentials_whenEmailNotFound() {
        var command = new AuthenticateUserCommand("unknown@example.com", "pass");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> useCase.execute(command));
        verify(tokenProvider, never()).generateToken(any(), anyCollection());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidCredentials_whenPasswordIsWrong() {
        var command = new AuthenticateUserCommand("john@example.com", "wrongpass");
        var user = new User(1L, "John", "john@example.com", "hashed", null, null);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> useCase.execute(command));
        verify(tokenProvider, never()).generateToken(any(), anyCollection());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldPassPermissionsCollectionToTokenProvider() {
        var command = new AuthenticateUserCommand("john@example.com", "secret123");
        var user = new User(1L, "John", "john@example.com", "hashed", null, null);
        var savedRefreshToken = new RefreshToken(1L, 1L, "token",
                LocalDateTime.now().plusSeconds(REFRESH_EXPIRATION_SECONDS), false);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(tokenProvider.generateToken(eq(user), anyCollection())).thenReturn("jwt.access.token");
        when(tokenProvider.getExpirationInSeconds()).thenReturn(900L);
        when(refreshTokenGenerator.generate()).thenReturn("token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedRefreshToken);

        useCase.execute(command);

        verify(tokenProvider).generateToken(eq(user), any(Collection.class));
    }
}
