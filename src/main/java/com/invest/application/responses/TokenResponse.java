package com.invest.application.responses;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresIn,
        Long refreshTokenExpiresIn
) {}
