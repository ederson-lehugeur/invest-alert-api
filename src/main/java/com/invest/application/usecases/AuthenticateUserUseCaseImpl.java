package com.invest.application.usecases;

import com.invest.application.commands.AuthenticateUserCommand;
import com.invest.application.ports.in.AuthenticateUserUseCase;
import com.invest.application.responses.TokenResponse;
import com.invest.domain.entities.RefreshToken;
import com.invest.domain.entities.User;
import com.invest.domain.exceptions.InvalidCredentialsException;
import com.invest.domain.ports.out.PasswordEncoder;
import com.invest.domain.ports.out.RefreshTokenGenerator;
import com.invest.domain.ports.out.TokenProvider;
import com.invest.domain.ports.out.repositories.RefreshTokenRepository;
import com.invest.domain.ports.out.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class AuthenticateUserUseCaseImpl implements AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final long refreshTokenExpirationSeconds;

    @Override
    public TokenResponse execute(AuthenticateUserCommand command) {
        log.info("M=execute, I=Iniciando autenticacao de usuario, email={}", command.email());

        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> {
                    log.warn("M=execute, W=Credenciais invalidas, email={}", command.email());
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            log.warn("M=execute, W=Senha incorreta, email={}", command.email());
            throw new InvalidCredentialsException();
        }

        Set<String> permissionNames = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName())
                .collect(Collectors.toSet());

        String accessToken = tokenProvider.generateToken(user, permissionNames);

        refreshTokenRepository.revokeAllByUserId(user.getId());
        LocalDateTime refreshExpiry = LocalDateTime.now().plusSeconds(refreshTokenExpirationSeconds);
        RefreshToken refreshToken = new RefreshToken(user.getId(), refreshTokenGenerator.generate(), refreshExpiry);
        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        log.info("M=execute, I=Usuario autenticado com sucesso, userId={}", user.getId());
        return new TokenResponse(
                accessToken,
                savedRefreshToken.getToken(),
                tokenProvider.getExpirationInSeconds(),
                refreshTokenExpirationSeconds
        );
    }
}
