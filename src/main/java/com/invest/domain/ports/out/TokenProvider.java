package com.invest.domain.ports.out;

import com.invest.domain.entities.User;

import java.util.Collection;
import java.util.List;

public interface TokenProvider {

    String generateToken(User user, Collection<String> permissionNames);

    Long extractUserId(String token);

    List<String> extractPermissions(String token);

    boolean validateToken(String token);

    long getExpirationInSeconds();
}
