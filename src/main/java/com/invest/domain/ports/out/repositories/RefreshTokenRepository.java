package com.invest.domain.ports.out.repositories;

import com.invest.domain.entities.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByToken(String token);

    void revokeAllByUserId(Long userId);
}
