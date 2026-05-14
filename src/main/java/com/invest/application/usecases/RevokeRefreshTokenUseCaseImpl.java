package com.invest.application.usecases;

import com.invest.application.commands.RevokeRefreshTokenCommand;
import com.invest.application.ports.in.RevokeRefreshTokenUseCase;
import com.invest.domain.entities.RefreshToken;
import com.invest.domain.exceptions.InvalidRefreshTokenException;
import com.invest.domain.ports.out.repositories.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RevokeRefreshTokenUseCaseImpl implements RevokeRefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void execute(RevokeRefreshTokenCommand command) {
        log.info("M=execute, I=Revogando refresh token (logout)");

        RefreshToken token = refreshTokenRepository.findByToken(command.refreshToken())
                .orElseThrow(() -> {
                    log.warn("M=execute, W=Refresh token nao encontrado para revogacao");
                    return new InvalidRefreshTokenException();
                });

        token.revoke();
        refreshTokenRepository.save(token);

        log.info("M=execute, I=Refresh token revogado com sucesso, userId={}", token.getUserId());
    }
}
