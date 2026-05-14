package com.invest.application.usecases;

import com.invest.application.commands.RefreshTokenCommand;
import com.invest.application.ports.in.RefreshTokenUseCase;
import com.invest.application.responses.TokenResponse;
import com.invest.domain.entities.RefreshToken;
import com.invest.domain.entities.User;
import com.invest.domain.exceptions.InvalidRefreshTokenException;
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
public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final long refreshTokenExpirationSeconds;

    @Override
    public TokenResponse execute(RefreshTokenCommand command) {
        log.info("M=execute, I=Renovando access token via refresh token");

        RefreshToken existing = refreshTokenRepository.findByToken(command.refreshToken())
                .orElseThrow(() -> {
                    log.warn("M=execute, W=Refresh token nao encontrado");
                    return new InvalidRefreshTokenException();
                });

        if (!existing.isValid()) {
            log.warn("M=execute, W=Refresh token invalido ou expirado, userId={}", existing.getUserId());
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> {
                    log.error("M=execute, E=Usuario nao encontrado para refresh token, userId={}", existing.getUserId());
                    return new InvalidRefreshTokenException();
                });

        Set<String> permissionNames = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName())
                .collect(Collectors.toSet());

        String newAccessToken = tokenProvider.generateToken(user, permissionNames);

        existing.revoke();
        refreshTokenRepository.save(existing);

        LocalDateTime newExpiry = LocalDateTime.now().plusSeconds(refreshTokenExpirationSeconds);
        RefreshToken newRefreshToken = new RefreshToken(user.getId(), refreshTokenGenerator.generate(), newExpiry);
        RefreshToken savedRefreshToken = refreshTokenRepository.save(newRefreshToken);

        log.info("M=execute, I=Access token renovado com sucesso, userId={}", user.getId());
        return new TokenResponse(
                newAccessToken,
                savedRefreshToken.getToken(),
                tokenProvider.getExpirationInSeconds(),
                refreshTokenExpirationSeconds
        );
    }
}
