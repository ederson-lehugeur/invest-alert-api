package com.invest.domain.ports.out;

import com.invest.domain.entities.User;

public interface TokenProvider {

    String generateToken(User usuario);

    Long extractUserId(String token);

    boolean validateToken(String token);

    long getExpirationInSeconds();
}
