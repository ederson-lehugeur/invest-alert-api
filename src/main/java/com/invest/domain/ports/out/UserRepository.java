package com.invest.domain.ports.out;

import com.invest.domain.entities.User;

import java.util.Optional;

public interface UserRepository {

    User save(User usuario);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
